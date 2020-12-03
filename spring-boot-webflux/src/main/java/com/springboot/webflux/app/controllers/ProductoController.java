package com.springboot.webflux.app.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.springboot.webflux.app.models.documents.Categoria;
import com.springboot.webflux.app.models.documents.Producto;
import com.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SessionAttributes("producto")
@Controller
public class ProductoController {
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

	@Autowired
	private ProductoService productoService;
	
	@Value("${C:\\Users\\richa\\Documents\\workspace-spring-tool-suite-4-4.8.1.RELEASE\\uploads\\}")
	private String filePath;
	
	//Model para buscar las categorias para el selec de thymeleaf
	@ModelAttribute("categorias")
	public Flux<Categoria> categorias(){
		return productoService.findAllCategory();
	}
	
	//LISTAR LOS DATOS DESDE EL FLUJO 
	@GetMapping({"/listar" ,"/"})
	public String Listar(Model model) {
		Flux<Producto> productos = productoService.findAllNameUpperCase();				
		model.addAttribute("productos" , productos);
		model.addAttribute("titulo" , "Listado de productos");
		
		return "listar";
	}
	
	@GetMapping("/file/img/{imagen:.+}")
	public Mono<ResponseEntity<Resource>> verImagen (@PathVariable String imagen) throws MalformedURLException{
		Path ruta = Paths.get(filePath).resolve(imagen).toAbsolutePath();
		
		Resource foto = new UrlResource(ruta.toUri());
		
		return Mono.just(
				ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + foto.getFilename() + "\"")
				.body(foto)				
				);
	}
	
	@GetMapping("/ver/{id}")
	public Mono<String> ver(Model model , @PathVariable String id){
		
		return productoService.findById(id)
				.doOnNext(prod -> {
					model.addAttribute("producto" , prod);
					model.addAttribute("titulo" , "Detalle Producto");
					
				}).switchIfEmpty( Mono.just(new Producto()))
				.flatMap( prod -> {
					if(prod.getId() ==  null) {
						return Mono.error(new InterruptedException("No existe el producto"));
					}
					return Mono.just(prod);
				}).then( Mono.just("ver"))
				.onErrorResume( error -> Mono.just("redirect:/listar?error=no+existe+el+producto")); 

	}
	
	@GetMapping("/form")
	public Mono<String> crear(Model model){
		model.addAttribute("producto" , new Producto());
		model.addAttribute("titulo" , "Formulario de Producto");		
		return Mono.just("form");
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id , Model model){		
		Mono<Producto> producotMono = productoService.findById(id).doOnNext( prod -> {
			log.info("Prodcuto: " + prod.getNombre());
		//defaultIfEmpty es para evitar pantalla de error cuando se agregue un id inexistente creando un nuevo registro
		}).defaultIfEmpty(new Producto()); 
		
		model.addAttribute("producto" , producotMono );
		model.addAttribute("titulo" , "Editar producto");		
		return Mono.just("form");
	}
	
	@GetMapping("/form-v2/{id}")
	public Mono<String> editar2Version(@PathVariable String id , Model model){		
		
		return productoService.findById(id).doOnNext( prod -> {
			log.info("Prodcuto: " + prod.getNombre());
			model.addAttribute("producto" , prod );
			model.addAttribute("titulo" , "Editar producto");
		}).defaultIfEmpty(new Producto())
		.flatMap(prod -> {
			if(prod.getId() ==  null) {
				return Mono.error(new InterruptedException("No existe el producto"));
			}
			return Mono.just(prod);
		})
		.then(Mono.just("form"))
		.onErrorResume( error -> Mono.just("redirect:/listar?error=no+existe+el+producto")); 
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(@Validated Producto producto , BindingResult result , Model model , @RequestPart(name="file") FilePart archivo  ,SessionStatus status){
		
		if(result.hasErrors()) {
			model.addAttribute("titulo" , "Editar producto");
			return Mono.just("form");
		}
		
		status.setComplete();
		Mono<Categoria> categoria = productoService.findAllCategorybyId(producto.getCategoria().getId());
		
		return categoria.flatMap( cat -> {
			if(producto.getCreateAt()==null) {
				producto.setCreateAt(new Date());
			}
			//TRATAMIENTO DEL ARCHIVO , IMAGEN, DOCUMENTO ETC
			if(!archivo.filename().isEmpty()) {
				producto.setImagen( UUID.randomUUID().toString() + "-" + archivo.filename()
				.replace("" ,"")
				.replace(":" ,"")
				.replace("\\" ,"")
				);
			}
			producto.setCategoria(cat);
			return productoService.save(producto);
		})
		.doOnNext( prod -> {
			log.info("Categorias Asignada: " + prod.getCategoria() + " idCat " + prod.getCategoria().getId() );
			log.info("Guardado exitosamente: " + prod.getNombre() + " ID " + prod.getId() );
		}).flatMap( prod -> {
			if(!archivo.filename().isEmpty()) {
				return archivo.transferTo(new File(filePath + prod.getImagen()));
			}
			return Mono.empty();
		})
		.thenReturn("redirect:/listar?success=Guardado+exitosamente");
	}
	
	@GetMapping("/eliminar/{id}")
	public Mono<String> eliminar(@PathVariable String id){
		return productoService.findById(id)
		.defaultIfEmpty(new Producto())
				.flatMap(prod -> {
					if(prod.getId() ==  null) {
						return Mono.error(new InterruptedException("No existe el producto a eliminar"));
					}
					return Mono.just(prod);
				})		
		.flatMap( prod -> {
			log.info(prod.getId());
			return productoService.delete(prod);
		})
				
		.then(Mono.just("redirect:/listar?success=producto+eliminado"))
		.onErrorResume( error -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar")); 

	}
	
	

	//LISTA LOS DATOS EN LBOQUES POR SEGUNDos - muestra por bloques de 2 registros por segundos habilitando la sobrecarga o contrapresion
	@GetMapping("/listar-data")
	public String ListarDataDriver(Model model) {
		Flux<Producto> productos = productoService.findAllNameUpperCase()
				.delayElements(Duration.ofSeconds(1)); //timempo del delay para trabajar la contrapresion
		
		model.addAttribute("productos" , new ReactiveDataDriverContextVariable(productos , 2)); //tamaño de la cantidad elementos
		model.addAttribute("titulo" , "Listado de productos");
		
		return "listar";
	}
	
	//REPITE EL LISTADO 5000 VECES se puede configurar con spring.thymeleaf.reactive.max-chunk-size=1024 en el application properties
	@GetMapping("/listar-full")
	public String ListarFull(Model model) {
		Flux<Producto> productos = productoService.findAllNameUpperCaseRepeat();
		
		model.addAttribute("productos" , productos ); 
		model.addAttribute("titulo" , "Listado de productos");
		
		return "listar";
	}
	
	//REPITE EL LISTADO 5000 VECES se puede configurar con spring.thymeleaf.reactive.chunked-mode-view-names=*chunk*en el application properties
	@GetMapping("/listar-chunk")
	public String ListarChunked(Model model) {
		Flux<Producto> productos = productoService.findAllNameUpperCaseRepeat();
		
		model.addAttribute("productos" , productos ); 
		model.addAttribute("titulo" , "Listado de productos");
		
		return "listar-chunk";
	}
	
	
}
