# Alioli-Http
This library was designed to ensure request will reach the server somewhen within a given time frame. It intercepts
requests using [OkHttp](https://github.com/square/okhttp). Using Android's
[WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager/) it will re-send previously failed
requests until they are successful.

## Setup
The following two steps shows you how to set it up:

### 1. Add request header
Add one of the `AlioliHttpConstant` as a header to your request. If you use Retrofit you can use annotation:

```
@Headers({AlioliHttpConstant.HEADER_DEFAULT})
```

or do it programmatically:

```
val headers = okhttp3.Headers.Builder()
    .add(AlioliHttpConstant.HEADER_KEY_VALID_UNTIL, timestampInMs)
    .build()

val request = okhttp3.Request.Builder()
    .headers(headers)
    .build() 
```

Each header in `AlioliHttpConstant` has the same name: `x-alioli-http-valid-until`. The header value is the time in
millis until it will expire.

### 2. Add Interceptor to your OkHttpClient
Create an interceptor, using the `AlioliHttpInterceptorFactory`, and add it to your applications interceptors of your
`OkHttpClient`.

```
val interceptor = AlioliHttpInterceptorFactory.create(context)

OkHttpClient.Builder().apply {
    addInterceptor(interceptor)
}.build()
```

The interceptor will look for the header name: `x-alioli-http-valid-until`. If it finds one, it will save that request
to the local database. Then it proceeds the request. If the response was successful the entry for the request in the
database is being deleted. If the response fails it will enqueue a one time work request to the WorkManager with a
default delay of 15 minutes.

**Note: The AlioliHttpInterceptor should be the last application interceptor! Reason, if you have other following
interceptors they might change the request. So the request, saved to the local database and being executed later by the
AlioliHttpWorker would be different from the original request send to the server.**

## AlioliHttpWorker
The `AlioliHttpWorker` will execute the enqueued work. It will look for requests stored to the local DB. If the database
is empty the Worker will return `Result.success()`. Any request that is not valid anymore (`x-alioli-http-valid-until`
timestamp is in the past) the worker removes that request from the local database without executing it. If the request
is still valid it will be executed. If the response is successful it deletes the request from the local database.

**Note: Each request from the worker will be executed sequentially.**

Finally the worker checks if the local database is empty. If so, it returns `Result.success()`. If there are still
requests (new ones came in, or failed requests) the worker returns `Result.retry()`.

# License
```
MIT License

Copyright (c) 2019 Sensorberg GmbH

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
