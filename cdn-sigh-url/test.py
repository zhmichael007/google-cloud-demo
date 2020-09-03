#!/usr/bin/env python
import datetime
import signurl

# https://www.epochconverter.com/
expire_ts = 1599191837
key_code = '2jymkR0hUiasu3Kqtr1dCA==' # head -c 16 /dev/urandom | base64 | tr +/ -_
key_name = 'cdn-sign-key-1'

dingo_url = "http://34.120.174.10/1.txt"
dingo_url_prefix_1 = "http://34.120.174.10/cdn1"
dingo_url_with_prefix = "http://34.120.174.10/cdn1/1.txt"
dingo_url_prefix_2 = "http://34.120.174.10/cdn1/"

def test_sign_url():
    signurl.sign_url(
        dingo_url,
        key_name,
        key_code,
        datetime.datetime.utcfromtimestamp(expire_ts))

def test_sign_url_prefix():
    signurl.sign_url_prefix(
        dingo_url_with_prefix,
        dingo_url_prefix_1,
        key_name,
        key_code,
        datetime.datetime.utcfromtimestamp(expire_ts))

def test_sign_cookie():
    signurl.sign_cookie(
        dingo_url_prefix_2,
        key_name,
        key_code,
        datetime.datetime.utcfromtimestamp(expire_ts))

test_sign_url()
test_sign_url_prefix()
test_sign_cookie()