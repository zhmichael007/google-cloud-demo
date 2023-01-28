Google Cloud CDN signed URLs使用说明的官方链接（需翻墙访问）
https://cloud.google.com/cdn/docs/using-signed-urls

另外可以参考Python的实现方案，用来验证C++写的sign URL是否正确  
https://github.com/zhmichael007/google-cloud-demo/tree/master/cdn-sigh-url  

```diff
- 注意：该代码为C++实现的Google Cloud CDN 签名URL的prototype，个人兴趣开发，不保证没有问题。生产场景使用之前请严格测试！
```

该代码在Debain下运行通过  

代码中hmac-sha1的签名依赖openssl的lib实现，所以需要下载编译openssl 1.1.1的库：
```
sudo apt-get install wget git -y
wget https://www.openssl.org/source/openssl-1.1.1i.tar.gz
tar -zxvf openssl-1.1.1i.tar.gz
cd openssl-1.1.1i
./config   
sudo make install 
```

在Google Cloud Load Balancing的管理控制台上，设置签名URL的key：
![image](https://github.com/zhmichael007/google-cloud-demo/blob/master/cdn-sign-url-cpp/image/3.png)  

```
git clone https://github.com/zhmichael007/google-cloud-demo.git
cd google-cloud-demo/cdn-sign-url-cpp
```

在sign_url.cpp中，设置自己需要签名的URL路径，设置在Google Cloud CDN中设置的签名key name和key value：
![image](https://github.com/zhmichael007/google-cloud-demo/blob/master/cdn-sign-url-cpp/image/1.png)  

在目录下修改Makefile 然后make，生成signurl可执行程序，运行signurl
![image](https://github.com/zhmichael007/google-cloud-demo/blob/master/cdn-sign-url-cpp/image/2.png)  



