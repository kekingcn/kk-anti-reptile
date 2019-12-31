# 适用于spring-boot项目的反爬虫组件kk-anti-reptile

## 1. 概述
kk-anti-reptile是[凯京科技](https://www.keking.com)研发的，适用于`基于spring-boot开发的分布式系统`的反爬虫组件。

## 2. 系统要求
* 基于spring-boot开发(spring-boot1.x, spring-boot2.x均可)
* 需要使用redis

## 3. 工作流程
kk-anti-reptile使用基于Servlet规范的的Filter对请求进行过滤，在其内部通过spring-boot的扩展点机制，实例化一个Filter，并注入到Spring容器FilterRegistrationBean中，通过Spring注入到Servlet容器中，从而实现对请求的过滤

在kk-anti-reptile的过滤Filter内部，又通过责任链模式，将各种不同的过滤规则织入，并提供抽象接口，可由调用方进行规则扩展

Filter调用则链进行请求过滤，如过滤不通过，则拦截请求，返回状态码`509`，并输出验证码输入页面，输出验证码正确后，调用过滤规则链对规则进行重置

目前规则链中有如下两个规则
### ip-rule
ip-rule通过时间窗口统计当前时间窗口内请求数，小于规定的最大请求数则可通过，否则不通过。时间窗口、最大请求数、ip白名单等均可配置
### ua-rule
ua-rule通过判断请求携带的User-Agent，得到操作系统、设备信息、浏览器信息等，可配置各种维度对请求进行过滤

## 4. 接入使用
后端接入非常简单，只需要引用kk-anti-reptile的maven依赖，并配置启用kk-anti-reptile即可  
加入maven依赖
```xml
<dependency>
    <groupId>cn.keking.project</groupId>
    <artifactId>kk-anti-reptile</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
配置启用 kk-anti-reptile
```properties
anti.reptile.manager.enabled=true
```
前端需要在统一发送请求的ajax处加入拦截，拦截到请求返回状态码`509`后弹出一个新页面，并把响应内容转出到页面中，然后向页面中传入后端接口`baseUrl`参数即可，以使用axios请求为例：
```javascript
import axios from 'axios';
import {baseUrl} from './config';

axios.interceptors.response.use(
  data => {
    return data;
  },
  error => {
    if (error.response.status === 509) {
      let html = error.response.data;
      let verifyWindow = window.open("","_blank","height=400,width=560");
      verifyWindow.document.write(html);
      verifyWindow.document.getElementById("baseUrl").value = baseUrl;
    }
  }
);

export default axios;
```
凯京科技内部用户可以访问 https://git.keking.cn/infrastructure/kk-antireptile-demo 查看接入示例

## 5. 注意
1. apollo-client需启用bootstrap

使用apollo配置中心的用户，由于组件内部用到`@ConditionalOnProperty`，要在application.properties/bootstrap.properties中加入如下样例配置，(apollo-client需要0.10.0及以上版本）详见[apollo bootstrap说明](https://github.com/ctripcorp/apollo/wiki/Java%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97#3213-spring-boot%E9%9B%86%E6%88%90%E6%96%B9%E5%BC%8F%E6%8E%A8%E8%8D%90)
```properties
apollo.bootstrap.enabled = true
```
2. 需要有Redisson连接

如果项目中有用到Redisson，kk-anti-reptile会自动获取RedissonClient实例对象; 如果没用到，需要在配置文件加入如下Redisson连接相关配置
```properties
spring.redisson.address=redis://192.168.1.204:6379
spring.redisson.password=xxx
```
## 6. 配置一览表
在spring-boot中，所有配置在配置文件都会有自动提示和说明，如下图  
![配置自动提示及说明](https://gitchenjh.github.io//post-images/1563505482779.png)  
所有配置都以`anti.reptile.manager`为前缀，如下为所有配置项及说明    

| NAME                     | 描述                                     | 默认值 | 示例                |
| ------------------------ | ---------------------------------------- | ------ | ------------------- |
| enabled                  | 是否启用反爬虫插件                       | true   | true                |
| include-urls             | 需要反爬的接口列表，以'/'开头，以','分隔 | 空     | /client,/user       |
| ip-rule.enabled          | 是否启用IP Rule                          | true   | true                |
| ip-rule.expiration-time  | 时间窗口长度(ms)                         | 5000   | 5000                |
| ip-rule.request-max-size | 单个时间窗口内，最大请求数               | 20     | 20                  |
| ip-rule.ignore-ip        | IP白名单，支持后缀'\*'通配，以','分隔    | 空     | 192.168.*,127.0.0.1 |
| ua-rule.enabled          | 是否启用User-Agent Rule                  | true   | true                |
| ua-rule.allowed-linux    | 是否允许Linux系统访问                    | false  | false               |
| ua-rule.allowed-mobile   | 是否允许移动端设备访问                   | true   | true                |
| ua-rule.allowed-pc       | 是否允许移PC设备访问                     | true   | true                |
| ua-rule.allowed-iot      | 是否允许物联网设备访问                   | false  | false               |
| ua-rule.allowed-proxy    | 是否允许代理访问                         | false  | false               |

## 7、命中规则后
命中爬虫和防盗刷规则后，会阻断请求，并生成接除阻断的验证码，验证码有多种组合方式，如果客户端可以正确输入验证码，则可以继续访问
![输入图片说明](https://images.gitee.com/uploads/images/2019/1231/165221_4a0f9d93_492218.png "屏幕截图.png")
