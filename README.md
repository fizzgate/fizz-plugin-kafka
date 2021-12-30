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
1. 在Fizz网关路由配置中选择使用本插件，并配置好kafka服务地址
2. 请求返回后插件会将推送结果回写到响应头中, fizz-plugin-kafka-result写回推送结果，1表示推送成功, 0表示推送失败;如果推送成功,插件通过fizz-plugin-kafka-metadata回写kafka metadata信息
3. 自定义发送topic:设置自定义header请求头fizz-plugin-kafka-topic传递kafka topic名称

---

###后续版本开发列表
1. 插件配置修改后立即生效
2. kafka推送内容可配置化
3. 优化异常响应的处理

