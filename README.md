## 概述

kk-anti-reptile 是适用于`基于 spring-boot 开发的分布式系统`的开源反爬虫接口防刷组件。

## 开源地址

[https://gitee.com/kekingcn/kk-anti-reptile](https://gitee.com/kekingcn/kk-anti-reptile)

[https://github.com/kekingcn/kk-anti-reptile](https://github.com/kekingcn/kk-anti-reptile)

## 系统要求

-   基于 spring-boot 开发(spring-boot1.x, spring-boot2.x 均可)
-   需要使用 redis

## 工作流程

kk-anti-reptile 使用 [SpringMVC拦截器](https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-handlermapping-interceptor) 对请求进行过滤，通过 spring-boot 的扩展点机制，实例化一个Spring HandlerInterceptor Bean，通过 Spring 注入到 Servlet 容器中，从而实现对请求的过滤

在 kk-anti-reptile 的过滤 Interceptor 内部，又通过责任链模式，将各种不同的过滤规则织入，并提供抽象接口，可由调用方进行规则扩展

Interceptor 调用则链进行请求过滤，如过滤不通过，则拦截请求，返回状态码`509`，并输出验证码输入页面，输出验证码正确后，调用过滤规则链对规则进行重置

目前规则链中有如下两个规则

### ip-rule

ip-rule 通过时间窗口统计当前时间窗口内请求数，小于规定的最大请求数则可通过，否则不通过。时间窗口、最大请求数、ip 白名单等均可配置

### ua-rule

ua-rule 通过判断请求携带的 User-Agent，得到操作系统、设备信息、浏览器信息等，可配置各种维度对请求进行过滤

## 验证码页面

命中爬虫和防盗刷规则后，会阻断请求，并生成接除阻断的验证码，验证码有多种组合方式，如果客户端可以正确输入验证码，则可以继续访问

![](https://kkfileview.keking.cn/anti-reptile/06114318_NlQW.png)

验证码有中文、英文字母+数字、简单算术三种形式，每种形式又有静态图片和 GIF 动图两种图片格式，即目前共有如下六种，所有类型的验证码会随机出现，目前技术手段识别难度极高，可有效阻止防止爬虫大规模爬取数据

![](https://kkfileview.keking.cn/anti-reptile/up-0e140d960cdf1771d71663dace1b3b0b151.png)![](https://kkfileview.keking.cn/anti-reptile/up-1e95900b91df071f7fe9c6e487e1d8ec3bb.gif)

![](https://kkfileview.keking.cn/anti-reptile/up-ede7ddd514dbd1be7744453cabd56a67e81.png)![](https://oscimg.oschina.net/oscnet/up-42a72529601c93ab4d7bbd43dc4b10ae795.gif)

![](https://kkfileview.keking.cn/anti-reptile/up-f63acf78d822e46b1d4a490fac235e5f098.png)![](https://kkfileview.keking.cn/anti-reptile/up-1884209f099a909b2839fddfa09ff7025f0.gif)

## 接入使用

接入非常简单，只需要引用 kk-anti-reptile 的 maven 依赖，并配置启用 kk-anti-reptile 即可  
### 1. 加入 maven 依赖

```xml
<dependency>
    <groupId>cn.keking.project</groupId>
    <artifactId>kk-anti-reptile</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>

```

### 2. 配置启用 kk-anti-reptile

在spring-boot配置文件中加入如下配置 `anti.reptile.manager.enabled`

```properties
anti.reptile.manager.enabled = true
```
### 3. 配置需要反爬的接口

配置反爬接口有如下两种方式，两种方式可以同时使用

1. 使用配置文件

在spring-boot配置文件中加入如下配置项`anti.reptile.manager.include-urls`，值为反爬的接口URI（如：/client/list），支持正则表达式匹配（如：^/admin/.*$），多项用`,`分隔
```properties
anti.reptile.manager.include-urls = /client/list,/user/list,^/admin/.*$
```

2. 使用注解

在需要反爬的接口Controller对象对应的接口上加上`@AntiReptile`注解即可，示例如下

```java
@RestController
@RequestMapping("/demo")
public class DemoController {

    @AntiReptile
    @GetMapping("")
    public String demo() {
        return "Hello，World!";
    }

}
```

### 4. 前端统一处理验证码页面

前端需要在统一发送请求的 ajax 处加入拦截，拦截到请求返回状态码`509`后弹出一个新页面，并把响应内容转出到页面中，然后向页面中传入后端接口`baseUrl`参数即可，以使用 axios 请求为例：

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

## 注意

1.  apollo-client 需启用 bootstrap

使用 apollo 配置中心的用户，由于组件内部用到`@ConditionalOnProperty`，要在 application.properties/bootstrap.properties 中加入如下样例配置，(apollo-client 需要 0.10.0 及以上版本）详见[apollo bootstrap 说明](https://github.com/ctripcorp/apollo/wiki/Java%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97#3213-spring-boot%E9%9B%86%E6%88%90%E6%96%B9%E5%BC%8F%E6%8E%A8%E8%8D%90)

```properties
apollo.bootstrap.enabled = true
```

1.  需要有 Redisson 连接

如果项目中有用到 Redisson，kk-anti-reptile 会自动获取 RedissonClient 实例对象; 如果没用到，需要在配置文件加入如下 Redisson 连接相关配置

```properties
spring.redisson.address = redis://192.168.1.204:6379
spring.redisson.password = xxx
```

## 配置一览表

在 spring-boot 中，所有配置在配置文件都会有自动提示和说明，如下图  
![配置自动提示及说明](https://kkfileview.keking.cn/anti-reptile/06114319_IJlq.png)  
所有配置都以`anti.reptile.manager`为前缀，如下为所有配置项及说明

| NAME | 描述 | 默认值 | 示例 |
| --- | --- | --- | --- |
| enabled | 是否启用反爬虫插件 | true | true |
| globalFilterMode | 是否启用全局拦截模式 | false | true |
| include-urls | 局部拦截时，需要反爬的接口列表，以','分隔，支持正则匹配。全局拦截模式下无需配置 | 空 | /client,/user,^/admin/.*$ |
| ip-rule.enabled | 是否启用 IP Rule | true | true |
| ip-rule.expiration-time | 时间窗口长度(ms) | 5000 | 5000 |
| ip-rule.request-max-size | 单个时间窗口内，最大请求数 | 20 | 20 |
| ip-rule.lock-expire | 命中规则后自动解除时间（单位：s） | 10天 | 20 |
| ip-rule.ignore-ip | IP 白名单，支持后缀'*'通配，以','分隔 | 空 | 192.168.*,127.0.0.1 |
| ua-rule.enabled | 是否启用 User-Agent Rule | true | true |
| ua-rule.allowed-linux | 是否允许 Linux 系统访问 | false | false |
| ua-rule.allowed-mobile | 是否允许移动端设备访问 | true | true |
| ua-rule.allowed-pc | 是否允许移 PC 设备访问 | true | true |
| ua-rule.allowed-iot | 是否允许物联网设备访问 | false | false |
| ua-rule.allowed-proxy | 是否允许代理访问 | false | false |

## 联系我们

使用过程中有任何问题，都可以加入官方 QQ 群：613025121 咨询讨论 

![官方 QQ 群](https://kkfileview.keking.cn/anti-reptile/qq.png)
