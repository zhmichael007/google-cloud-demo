LNK_OPT = -g -L/usr/lib64/ -lssl  -L/lib64/ -lcrypto
 
all:
	g++ -g sign_url.cpp -o signurl $(LNK_OPT)
 
clean:
	rm -f *.o
	rm -f signurl

