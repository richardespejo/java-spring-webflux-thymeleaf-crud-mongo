package com.springboot.webflux.app.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springboot.webflux.app.models.dao.ProductoDao;
import com.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

	@Autowired
	private ProductoDao daoProducto;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoRestController.class);
	
	@GetMapping()
	public Flux<Producto> index(){
		
			Flux<Producto> productos = daoProducto.findAll().map( producto -> {
				producto.setNombre(producto.getNombre().toUpperCase());
				return producto;
			
			}).doOnNext( prod -> log.info(prod.getNombre()));		
		return productos;
		
	}
	
	@GetMapping("/{id}")
	public Mono<Producto> mostrar(@PathVariable String id){
		
		//FORMA EFICIENTE
		//Mono<Producto> producto = daoProducto.findById(id);
		
		//FORMA ALTERNATIVA
		//por medio de un flux aplico un filter indicando que lo que viene por parametro sea igual a la condicion del filtro
		Flux<Producto> productos = daoProducto.findAll();
		Mono<Producto> producto = productos.filter( p -> p.getId().equals(id))
				.next()
				.doOnNext(p -> log.info(p.getNombre()));
				
		return producto;
		
	}
	
}
