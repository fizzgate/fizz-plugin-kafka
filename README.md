# fizz-push-message-plugin
### 插件功能
将请求和响应转发到kafka消息服务

---

### 功能特性
- 本插件基于reactor-kafka
- 可通过application.yml或者fizz网关管理后台配置
- 插件优先使用fizz网关管理后台配置,fizz网关管理后台配置项会覆盖application.yml中的配置项
- 配置参数与springboot中KafkaAutoConfiguration中使用到KafkaProperties保持一致
- 路由设置维度的配置参数调整会在10分钟后生效
- 推送到kafka的消息内容包括requestId, method, headers,path, ip, cookies, body, response body等

---

### 插件配置
使用本插件需要通过以下参数配置kafka服务地址和topic名称(application.yml,Fizz管理后台插件配置或者路由配置中设置),其他配置项可参考KafkaProperties类中定义的相关属性。
+ spring.kafka.bootstrap-servers
+ fizz-plugin-kafka-topic

---

### 插件使用
1. gateway项目pom文件中引入以下依赖
    ```
        <dependency>
	    <groupId>com.fizzgate</groupId>
            <artifactId>fizz-plugin-kafka</artifactId>
            <version>1.0</version>
        </dependency>     
    ```
2. 管理后台导入以下SQL
    ```
        INSERT INTO `tb_plugin` (`fixed_config`, `eng_name`, `chn_name`, `config`, `order`, `instruction`, `type`, `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`, `status`, `is_deleted`) VALUES ('', 'kafkaMessagePlugin', '根据插件的自定义规则，把请求转换为消息推送到 Kafka 或 RabbitMQ', '[{"field":"blockSourceRequest","label":"阻断原请求","component":"select","dataType":"string","value":"false","options":[{"label":"是","value":"true"},{"label":"否","value":"false"}],"desc":"选择是否需要阻断原来的请求，选择是的话，请求将不会被转发，而是只将请求转化为消息并投递到消息中间件中","placeholder":"请选择","rules":[{"required":true,"message":"请选择是否阻断原请求","trigger":"change"}]},{"field":"messageQueueType","label":"队列类型","component":"select","dataType":"string","options":[{"label":"RabbitMQ","value":"RabbitMQ"},{"label":"Kafka","value":"Kafka"}],"desc":"选择要转发的消息队列中间件","placeholder":"请选择","rules":[{"required":true,"message":"队列类型不能为空","trigger":"change"}]},{"field":"addresses","label":"服务地址","component":"input","dataType":"string","desc":"服务器地址及端口，如有多个，中间以英文逗号隔开","placeholder":"请输入地址","rules":[{"required":true,"message":"服务地址不能为空","trigger":"change"}]},{"field":"topics","label":"目标 topic (exchange)","component":"input","dataType":"string","desc":"发送的目标 topic 或者 exchange，多个topic以英文逗号隔开，并且会以轮询的方式发送消息","placeholder":"请输入地址","rules":[{"required":true,"message":"目标 topic (exchange) 不能为空","trigger":"change"}]},{"field":"username","label":"用户名","component":"input","dataType":"string","desc":"rabbitMq需要填写用户名","placeholder":"请输入"},{"field":"password","label":"密码","component":"input","dataType":"string","desc":"rabbitMq需要填写密码","placeholder":"请输入"}]', 255, '', 2, NULL, NULL, NULL, NULL, NULL, 1, 0);
    ```
3. 在Fizz网关路由配置中选择使用本插件，并配置好kafka服务地址
4. 请求返回后插件会将推送结果回写到响应头中, fizz-plugin-kafka-result写回推送结果，1表示推送成功, 0表示推送失败;如果推送成功,插件通过fizz-plugin-kafka-metadata回写kafka metadata信息
5. 自定义发送topic:设置自定义header请求头fizz-plugin-kafka-topic传递kafka topic名称

### 后续版本开发列表
1. 插件配置修改后立即生效
2. kafka推送内容可配置化
3. 优化异常响应的处理

