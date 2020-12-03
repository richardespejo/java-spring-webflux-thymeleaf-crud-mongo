package com.springboot.webflux.app;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.springboot.webflux.app.models.documents.Categoria;
import com.springboot.webflux.app.models.documents.Producto;
import com.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {
	
	private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

	@Autowired
	private ProductoService productoService;
	
	@Autowired
	private ReactiveMongoTemplate mongoTemplate;
	
	public static void main(String[] args)  {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		//elimino todos los registro de la coleccion de mongo
		mongoTemplate.dropCollection("productos").subscribe();
		mongoTemplate.dropCollection("categorias").subscribe();
		
		Categoria electronica = new Categoria("Electronico");
		Categoria computacion = new Categoria("Computacion");
		Categoria telefonia = new Categoria("Telefonia");
		
		Flux.just(electronica , computacion , telefonia)
		.flatMap( categoria -> productoService.saveCategory(categoria))
		.doOnNext(cat -> {
			log.info("categoria creada: " + cat.getDescripcion() + " id " + cat.getId());
		}).thenMany(
				
				//añado datos de prueba a la coleccion Productos de mongo
				Flux.just( new Producto("Laptop asus TUF gaming A15" , 1.400 , computacion),
						new Producto("Play Station 5" , 500 , electronica),
						new Producto("Xbox X series" , 480 , electronica),
						new Producto("Monitor LG 144hz" , 120.30 , computacion),
						new Producto("Mac Book Pro M1" , 1.600 , computacion),
						new Producto("Combo Teclado / raton gaming Mars Gaming" , 40 , computacion),
						new Producto("Laptop portatil Dell Latitude 230G" , 620.25 , computacion),
						new Producto("Smartphone Xiaomi A2lite" , 190 , telefonia),
						new Producto("Smartphone Xiaomi redmi 9A" , 320 , telefonia),
						new Producto("Ipega Mando PC/ps3/xbox360/android/IOS" , 35.75 , computacion)			
						
						)
				.flatMap( producto -> {
					producto.setCreateAt(new Date());
					return productoService.save(producto);
					})
			)
			.subscribe( producto -> log.info("insert " + producto.getId() + " " + producto.getNombre() ));
	
		
		
		
	}

}
