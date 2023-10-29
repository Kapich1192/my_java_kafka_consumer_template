# Шаблон ***Kafka Consumer***
1. [Определения](#определения)
    * [Kafka](#kafka)
    * [Топики](#топики)
    * [Разделы](#разделы)
    * [Templates](#шаблон)
2. [Архитектура](#архитектура)
3. [Запуск в Docker](#запуск-в-docker)
4. [Consumer](#consumer)

## Определения
### Kafka
Кластер Kafka обладает высокой масштабируемостью и отказоустойчивостью:
при поломке одного из узлов, другие узлы берут на себя его работу,
обеспечивая непрерывность работы без потери данных.

Чтение и запись данных в Kafka выполняется в виде событий,
содержащих информацию в различном формате, например, в виде строки,
массива или JSON-объекта.

**Producer** (производитель, издатель) публикует (записывает) события в Kafka,
а **Consumer** (потребитель, подписчик) подписывается на эти события и обрабатывает их.

### Топики
События группируются в топики (topic). Топик похож на папку, а события — на файлы в этой папке. 
У топика может быть ноль, один или много издателей и подписчиков.

События можно прочитать столько раз, сколько необходимо.
В этом отличие Kafka от традиционных систем обмена сообщениями:
после чтения события не удаляются. Можно настроить, как долго Kafka хранит события.

### Разделы
Топики поделены на разделы (partition). Публикация события в топике фактически означает
добавление его к одному из разделов. События с одинаковыми ключами записываются в один раздел.
В рамках раздела Kafka гарантирует порядок событий.


![Topics](img/2_topic.png)

Для отказоустойчивости и высокой доступности топик может быть реплицирован,
в том числе между различными, географически удаленными, датацентрами.
То есть всегда будет несколько брокеров с копиями данных на случай, если что-то пойдет не так.

### Шаблон


![Consumer](img/3_consumer.png)


## Архитектура


![Архитектура](img/1_arh.png)

Producer-микросервис ("писатель"), который получает сообщения
и передает их через Kafka в Consumer-микросервис ("читатель") для сохранения их в БД.

## Запуск в Docker

Параметры запуска Kafka, Kafdrop и Zookeeper в докере

```yaml
version: "3.7"

networks:
  kafka-net:
    name: kafka-net
    driver: bridge

services:
  zookeeper:
    image: zookeeper:3.7.0
    container_name: zookeeper
    restart: "no"
    networks:
      - kafka-net
    ports:
      - "2181:2181"

  kafka:
    image: obsidiandynamics/kafka
    container_name: kafka
    restart: "no"
    networks:
      - kafka-net
    ports:
      - "9092:9092"
    environment:
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: DOCKER_INTERNAL:PLAINTEXT,DOCKER_EXTERNAL:PLAINTEXT
      KAFKA_LISTENERS: DOCKER_INTERNAL://:29092,DOCKER_EXTERNAL://:9092
      KAFKA_ADVERTISED_LISTENERS: DOCKER_INTERNAL://kafka:29092,DOCKER_EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: DOCKER_INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zookeeper:2181"
      KAFKA_BROKER_ID: 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper

  kafdrop:
    image: obsidiandynamics/kafdrop
    container_name: kafdrop
    restart: "no"
    networks:
      - kafka-net
    ports:
      - "9000:9000"
    environment:
      KAFKA_BROKERCONNECT: "kafka:29092"
    depends_on:
      - "kafka"
```

## Consumer

![Consumer ar](img/5_consumer_ar.png)

Этапы создания Consumer-микросервиса:

* конфигурируем group-id и бины;
* настраиваем доступ к базе данных;
* создаем Consumer и OrderService;
* создаем репозиторий OrderRepository.

Конфигурации Consumer-микросервиса и  базы данных.

```yaml
server:
  port: 8081

topic:
  name: t.food.order

spring:
  kafka:
    consumer:
      group-id: "default"

  h2:
    console:
      enabled: true
      path: /h2-console
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: password
```
Config отвечает за настройку бина ModelMapper — библиотеки для маппинга
одних объектов на другие. Например, для DTO, используемого далее.

```java
@Configuration
public class Config {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
```

Классы модели:

```java
@Data
@Value
public class OrderDto {
    String item;
    Double amount;
}
```

```java
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String item;
    private Double amount;
}
```

Consumer отвечает за прослушивание топика с ордерами и получение сообщений. 
Полученные сообщения мы преобразуем в OrderDto, не содержащего ничего,
связанного с персистентностью, например, ID.

```java
@Slf4j
@Component
public class Consumer {

    private static final String orderTopic = "${topic.name}";

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @Autowired
    public Consumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = orderTopic)
    public void consumeMessage(String message) throws JsonProcessingException {
        log.info("message consumed {}", message);

        OrderDto orderDto = objectMapper.readValue(message, OrderDto.class);
        orderService.persistOrder(orderDto);
    }

}
```

OrderService — преобразование полученного DTO в объект Order и сохранение его в БД.

```java
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public OrderService(OrderRepository orderRepository, ModelMapper modelMapper) {
        this.orderRepository = orderRepository;
        this.modelMapper = modelMapper;
    }

    public void persistOrder(OrderDto orderDto) {
        Order order = modelMapper.map(orderDto, Order.class);
        Order persistedOrder = orderRepository.save(order);

        log.info("order persisted {}", persistedOrder);
    }

}
```

Код OrderRepository:

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
```
---

Дополнительные файлы и инфа содержаться в папке materials в корне проекта