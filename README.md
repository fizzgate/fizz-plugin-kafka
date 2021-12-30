# fizz-push-message-plugin
###插件功能
将请求和响应转发到kafka消息服务

---

###功能特性
- 本插件基于reactor-kafka
- 可通过application.yml或者fizz网关管理后台配置
- 插件优先使用fizz网关管理后台配置,fizz网关管理后台配置项会覆盖application.yml中的配置项
- 配置参数与springboot中KafkaAutoConfiguration中使用到KafkaProperties保持一致
- 路由设置维度的配置参数调整会在10分钟后生效
- 推送到kafka的消息内容包括requestId, method, headers,path, ip, cookies, body, response body等

---

###插件配置
使用本插件需要通过以下参数配置kafka服务地址和topic名称(application.yml,Fizz管理后台插件配置或者路由配置中设置),其他配置项可参考KafkaProperties类中定义的相关属性。
+ spring.kafka.bootstrap-servers
+ fizz-plugin-kafka-topic

---

###插件使用
1. gateway项目pom文件中引入以下依赖
        ```
                <dependency>
                        <groupId>com.fizzgate</groupId>
                        <artifactId>替换实际的插件artifactId</artifactId>
                        <version>替换实际的插件version</version>
                </dependency>        
        ```
2. 管理后台导入以下SQL
        ```
                INSERT INTO `tb_plugin` (`fixed_config`, `eng_name`, `chn_name`, `config`, `order`, `instruction`, `type`, `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`, `status`, `is_deleted`) VALUES 
                ('', 'fizzPluginKafka', 'Kafka消息插件', '', 1, '插件使用说明', 2, NULL, NULL, NULL, NULL, NULL, 1, 0);
3. 在Fizz网关路由配置中选择使用本插件，并配置好kafka服务地址
4. 请求返回后插件会将推送结果回写到响应头中, fizz-plugin-kafka-result写回推送结果，1表示推送成功, 0表示推送失败;如果推送成功,插件通过fizz-plugin-kafka-metadata回写kafka metadata信息
5. 自定义发送topic:设置自定义header请求头fizz-plugin-kafka-topic传递kafka topic名称

###后续版本开发列表
1. 插件配置修改后立即生效
2. kafka推送内容可配置化
3. 优化异常响应的处理

