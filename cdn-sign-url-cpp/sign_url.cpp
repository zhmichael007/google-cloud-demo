/*
THis code dependcy on openssl lib, apt-get install openssl
*/


#define FILE_URL "http://34.117.201.214/1.png"
#define CDN_KEY_NAME "cdn-sign-key-1"
#define CDN_KEY_VALUE "2jymkR0hUiasu3Kqtr1dCA=="

#include <iostream>
#include <string>
#include <ctime>
#include <stdio.h>
#include <string.h>
#include <algorithm>
#include <openssl/hmac.h>

using namespace std;

//https://web.mit.edu/freebsd/head/contrib/wpa/src/utils/base64.c , change os_malloc, os_memset to malloc, memset
static const unsigned char base64_table[65] =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

static const unsigned char url_base64_table[65] =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

/**
 * base64_encode - Base64 encode
 * @src: Data to be encoded
 * @len: Length of the data to be encoded
 * @out_len: Pointer to output length variable, or %NULL if not used
 * Returns: Allocated buffer of out_len bytes of encoded data,
 * or %NULL on failure
 *
 * Caller is responsible for freeing the returned buffer. Returned buffer is
 * nul terminated to make it easier to use as a C string. The nul terminator is
 * not included in out_len.
 */
static unsigned char * base64_encode(const unsigned char *src, size_t len,
			      size_t *out_len, bool url_encode=false)
{
	unsigned char *out, *pos;
	const unsigned char *end, *in;
	size_t olen;
	int line_len;
    const unsigned char * table = url_encode ? url_base64_table:base64_table;
    
	olen = len * 4 / 3 + 4; /* 3-byte blocks to 4-byte */
	olen += olen / 72; /* line feeds */
	olen++; /* nul termination */
	if (olen < len)
		return NULL; /* integer overflow */
	out = (unsigned char*)malloc(olen);
	if (out == NULL)
		return NULL;

	end = src + len;
	in = src;
	pos = out;
	line_len = 0;
	while (end - in >= 3) {
		*pos++ = table[in[0] >> 2];
		*pos++ = table[((in[0] & 0x03) << 4) | (in[1] >> 4)];
		*pos++ = table[((in[1] & 0x0f) << 2) | (in[2] >> 6)];
		*pos++ = table[in[2] & 0x3f];
		in += 3;
		line_len += 4;
		if (line_len >= 72) {
			*pos++ = '\n';
			line_len = 0;
		}
	}

	if (end - in) {
		*pos++ = table[in[0] >> 2];
		if (end - in == 1) {
			*pos++ = table[(in[0] & 0x03) << 4];
			*pos++ = '=';
		} else {
			*pos++ = table[((in[0] & 0x03) << 4) |
					      (in[1] >> 4)];
			*pos++ = table[(in[1] & 0x0f) << 2];
		}
		*pos++ = '=';
		line_len += 4;
	}

	if (line_len)
		*pos++ = '\n';

	*pos = '\0';
	if (out_len)
		*out_len = pos - out;
	return out;
}


/**
 * base64_decode - Base64 decode
 * @src: Data to be decoded
 * @len: Length of the data to be decoded
 * @out_len: Pointer to output length variable
 * Returns: Allocated buffer of out_len bytes of decoded data,
 * or %NULL on failure
 *
 * Caller is responsible for freeing the returned buffer.
 */
static unsigned char * base64_decode(const unsigned char *src, size_t len,
			      size_t *out_len)
{
	unsigned char dtable[256], *out, *pos, block[4], tmp;
	size_t i, count, olen;
	int pad = 0;

	memset(dtable, 0x80, 256);
	for (i = 0; i < sizeof(base64_table) - 1; i++)
		dtable[base64_table[i]] = (unsigned char) i;
	dtable['='] = 0;

	count = 0;
	for (i = 0; i < len; i++) {
		if (dtable[src[i]] != 0x80)
			count++;
	}

	if (count == 0 || count % 4)
		return NULL;

	olen = count / 4 * 3;
	pos = out = (unsigned char *)malloc(olen);
	if (out == NULL)
		return NULL;

	count = 0;
	for (i = 0; i < len; i++) {
		tmp = dtable[src[i]];
		if (tmp == 0x80)
			continue;

		if (src[i] == '=')
			pad++;
		block[count] = tmp;
		count++;
		if (count == 4) {
			*pos++ = (block[0] << 2) | (block[1] >> 4);
			*pos++ = (block[1] << 4) | (block[2] >> 2);
			*pos++ = (block[2] << 6) | block[3];
			count = 0;
			if (pad) {
				if (pad == 1)
					pos--;
				else if (pad == 2)
					pos -= 2;
				else {
					/* Invalid padding */
					free(out);
					return NULL;
				}
				break;
			}
		}
	}

	*out_len = pos - out;
	return out;
}

//duration : int: seconds
/**
 * create_signed_url - sign url
 * @url: url to be signed
 * @key_name: key name set in the Google Cloud CDN
 * @key_value: key value of the key_name in Google Cloud CDN
 * @duration: expire time of this signature, unit is second
 * Returns: A signed url with this format: https://example.com/foo?Expires=EXPIRATION&KeyName=KEY_NAME&Signature=SIGNATURE
 * or empty string on failure
 *
 */
static string create_signed_url(string url, string key_name, string key_value, int duration=3600)
{
    time_t now = time(0);
    time_t expire_time = now + duration;
    cout<<"current time: "<<now<<", expiration time: "<<expire_time<<endl;

    string url_to_sign = url + (url.find("?")!=string::npos?"&" : "?") + "Expires=" + std::to_string(expire_time) + "&KeyName=" + key_name;

    cout<< "url to sign: "<<url_to_sign<<endl;

    //hmac-sha1
    unsigned char digest[EVP_MAX_MD_SIZE] = {'\0'};
    unsigned int digest_len = 0;
    size_t out_len = 0;
    unsigned char* decoded_key_value = base64_decode((unsigned char*)key_value.c_str(), key_value.length(), &out_len);
    if(decoded_key_value == NULL)
    {
        cout << "malformed base64 key value"<<endl;
        return "";
    }
    HMAC(EVP_sha1(), decoded_key_value, out_len, (const unsigned char*)url_to_sign.c_str(), url_to_sign.length(), digest, &digest_len);
    free(decoded_key_value);

    //url base64 encode for digest
    unsigned char* base64_url_encoded_digest = base64_encode((unsigned char*)digest, digest_len, &out_len, true);
    if(NULL == base64_url_encoded_digest)
    {
        cout << "base64 encode digest failed" <<endl;
        return "";
    }
    printf("base64 url encoded digest: %s", base64_url_encoded_digest);
    string str_encoded_digest = (char*)base64_url_encoded_digest;
    free(base64_url_encoded_digest);

    return url_to_sign+"&Signature="+str_encoded_digest;
}

int main()
{
    std::cout << create_signed_url(FILE_URL, CDN_KEY_NAME, CDN_KEY_VALUE, 3600)<<std::endl;
    return 0;
}