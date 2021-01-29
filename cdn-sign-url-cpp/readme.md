```diff
- 注意：该代码为C++实现的Google Cloud CDN 签名URL的prototype，个人兴趣开发，不保证没有问题。生成场景使用之前请严格测试！
```

该代码在Debain下运行通过  

代码中hmac-sha1的签名依赖openssl的lib实现，所以需要下载编译openssl的库：   
- git clone git://git.openssl.org/openssl.git   
- 进入openssl目录，运行 ./config   
- sudo make install   
- sudo cp *.so.3 /usr/lib/  

在Google Cloud Load Balancing的管理控制台上，设置签名URL的key：
![image](https://github.com/zhmichael007/google-cloud-demo/blob/master/cdn-sign-url-cpp/image/3.png)  

在sign_url.cpp中，设置自己需要签名的URL路径，设置在Google Cloud CDN中设置的签名key name和key value：
![image](https://github.com/zhmichael007/google-cloud-demo/blob/master/cdn-sign-url-cpp/image/1.png)  

在目录下修改Makefile 然后make，生成signurl可执行程序，运行signurl
![image](https://github.com/zhmichael007/google-cloud-demo/blob/master/cdn-sign-url-cpp/image/2.png)  



