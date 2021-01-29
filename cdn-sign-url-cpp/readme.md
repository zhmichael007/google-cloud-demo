该代码在Debain下运行通过

代码中hmac-sha1的签名依赖openssl的lib实现，所以需要下载变异openssl的库： 
git clone git://git.openssl.org/openssl.git 
进入openssl目录，运行 ./config 
sudo make install 
sudo cp *.so.3 /usr/lib/

在目录下修改Makefile 然后make，运行signurl
