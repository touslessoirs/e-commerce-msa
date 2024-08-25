//package com.project.productservice.messageQueue;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.project.productservice.entity.Product;
//import com.project.productservice.repository.ProductRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@Slf4j
//public class KafkaConsumer {
//    private final ProductRepository productRepository;
//
//    public KafkaConsumer(ProductRepository productRepository) {
//        this.productRepository = productRepository;
//    }
//
//    /**
//     * product entity 전달 -> 해당 id의 stock(quantity) update
//     *
//     * @param kafkaMessage
//     */
//    @KafkaListener(topics = "test-product-topic")
//    public void updateProduct(String kafkaMessage) {
//        log.info("kafkaMessage : {}", kafkaMessage);
//
//        //kafka message(stream) -> json 변환
//        Map<Object, Object> map = new HashMap<>();
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            map = mapper.readValue(kafkaMessage, new TypeReference<Map<Object, Object>>() {});
//        } catch (JsonProcessingException ex) {
//            ex.printStackTrace();
//        }
//
//        // productId 추출
//        Long productId = Long.valueOf(map.get("productId").toString());
//
//        Product product = productRepository.findById(productId).orElse(null);
//        if (product != null) {
//            product.setStock(product.getStock() - (Integer)map.get("quantity"));
//            productRepository.save(product);
//        }
//    }
//}
