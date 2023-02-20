import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.SenderResult;
import com.fizzgate.plugin.PluginConfig;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.message.FizzKafkaProducer;
import com.fizzgate.plugin.message.FizzKafkaProducerFactory;
import com.fizzgate.plugin.message.KafkaMessagePluginFilter;
import com.fizzgate.plugin.message.Main;
import com.fizzgate.plugin.requestbody.RequestBodyPlugin;
import com.fizzgate.util.JacksonUtils;

import javax.annotation.Resource;
import java.util.*;

@SpringBootTest(classes = Main.class)
@RunWith(SpringRunner.class)
public class ApplicationTest {

    @Resource(name = "fizzKafkaProducerFactory")
    private FizzKafkaProducerFactory fizzKafkaProducerFactory;

    //插件维度设置
    public void testConfig1() {
        List<ApiConfig> apiConfigs = new ArrayList<>();
        ApiConfig ac = new ApiConfig(); // 一个路由配置
        ac.id = 1666; // 路由 id，建议从 1000 开始
        ac.service = "plateno-ota-api"; // 前端服务名
        ac.path = "/booking/**"; // 前端路径
        ac.type = ApiConfig.Type.REVERSE_PROXY; // 路由类型，此处为反向代理
        ac.httpHostPorts = Collections.singletonList("http://172.25.63.152:8081"); // 被代理接口的地址
        ac.backendPath = "/plateno-ota-api/booking/{$1}"; // 被代理接口的路径
        ac.pluginConfigs = new ArrayList<>();

        // 如果你的插件需要访问请求体，则首先要把 RequestBodyPlugin.REQUEST_BODY_PLUGIN 加到 ac.pluginConfigs 中，就像下面这样
        PluginConfig pc1 = new PluginConfig();
        pc1.plugin = RequestBodyPlugin.REQUEST_BODY_PLUGIN;
        ac.pluginConfigs.add(pc1);

        PluginConfig pc2 = new PluginConfig();
        pc2.plugin = KafkaMessagePluginFilter.KAFKA_MESSAGE_PLUGIN; // 应用 id 为 demoPlugin 的插件
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("max.block.ms", 3000l);
        pc2.setConfig(JacksonUtils.writeValueAsString(config));
        ac.pluginConfigs.add(pc2);
        apiConfigs.add(ac);
        Assert.assertEquals("topic1", pc2.config.get("fizz-plugin-kafka-topic"));
    }

    //应用维度设置
    public void testConfig2() {
        List<ApiConfig> apiConfigs = new ArrayList<>();
        ApiConfig ac = new ApiConfig(); // 一个路由配置
        ac.id = 1666; // 路由 id，建议从 1000 开始
        ac.service = "plateno-ota-api"; // 前端服务名
        ac.path = "/booking/**"; // 前端路径
        ac.type = ApiConfig.Type.REVERSE_PROXY; // 路由类型，此处为反向代理
        ac.httpHostPorts = Collections.singletonList("http://172.25.63.152:8081"); // 被代理接口的地址
        ac.backendPath = "/plateno-ota-api/booking/{$1}"; // 被代理接口的路径
        ac.pluginConfigs = new ArrayList<>();

        // 如果你的插件需要访问请求体，则首先要把 RequestBodyPlugin.REQUEST_BODY_PLUGIN 加到 ac.pluginConfigs 中，就像下面这样
        PluginConfig pc1 = new PluginConfig();
        pc1.plugin = RequestBodyPlugin.REQUEST_BODY_PLUGIN;
        ac.pluginConfigs.add(pc1);

        PluginConfig pc2 = new PluginConfig();
        pc2.plugin = KafkaMessagePluginFilter.KAFKA_MESSAGE_PLUGIN; // 应用 id 为 demoPlugin 的插件
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("max.block.ms", 3000l);
        pc2.setConfig(JacksonUtils.writeValueAsString(config));
        ac.pluginConfigs.add(pc2);
        apiConfigs.add(ac);
        Assert.assertEquals("topic2", pc2.config.get("fizz-plugin-kafka-topic"));
    }

    //路由维度设置
    public void testConfig3() {
        List<ApiConfig> apiConfigs = new ArrayList<>();
        ApiConfig ac = new ApiConfig(); // 一个路由配置
        ac.id = 1666; // 路由 id，建议从 1000 开始
        ac.service = "plateno-ota-api"; // 前端服务名
        ac.path = "/booking/**"; // 前端路径
        ac.type = ApiConfig.Type.REVERSE_PROXY; // 路由类型，此处为反向代理
        ac.httpHostPorts = Collections.singletonList("http://172.25.63.152:8081"); // 被代理接口的地址
        ac.backendPath = "/plateno-ota-api/booking/{$1}"; // 被代理接口的路径
        ac.pluginConfigs = new ArrayList<>();

        // 如果你的插件需要访问请求体，则首先要把 RequestBodyPlugin.REQUEST_BODY_PLUGIN 加到 ac.pluginConfigs 中，就像下面这样
        PluginConfig pc1 = new PluginConfig();
        pc1.plugin = RequestBodyPlugin.REQUEST_BODY_PLUGIN;
        ac.pluginConfigs.add(pc1);

        PluginConfig pc2 = new PluginConfig();
        pc2.plugin = KafkaMessagePluginFilter.KAFKA_MESSAGE_PLUGIN; // 应用 id 为 demoPlugin 的插件
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("max.block.ms", 3000l);
        pc2.setConfig(JacksonUtils.writeValueAsString(config));
        ac.pluginConfigs.add(pc2);
        apiConfigs.add(ac);
        Assert.assertEquals("topic3", pc2.config.get("fizz-plugin-kafka-topic"));
    }

    //构建消息消费工厂,发送消息
    public void testConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put("fizz-plugin-kafka-topic", "testTopic");
        ApiConfig apiConfig = new ApiConfig();
        FizzKafkaProducer fizzKafkaProducer = fizzKafkaProducerFactory.get(apiConfig, config);
        fizzKafkaProducer.sendMessage((String) config.get("fizz-plugin-kafka-topic"), "test message");

    }
}
