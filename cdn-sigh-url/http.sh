#!/bin/bash

http --follow \
    --print=HhBb \
    GET \
    http://35.241.61.244/cdn1/1.txt \
    'cookie: Cloud-CDN-Cookie=URLPrefix=aHR0cDovLzM1LjI0MS42MS4yNDQvY2RuMS8=:Expires=1594189564:KeyName=michaelzhkey:Signature=LYWkrlGl0rQXNLam7zLAhZTl25w='
