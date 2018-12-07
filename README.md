# Android app "Speed Read"

Speed Read は **電子書籍の速読支援アプリ**です。

Amazon Kindle アプリなど他の電子書籍アプリの画面上で動作します。

文章を読み取り、フラッシュ演算のように高速に「細かく分けた文章」を表示することで、目の移動の時間ロスをなくし速読に寄与します。

## 仕組み

1. Kindleアプリなど表示している状態で画面のスクショを撮る
2. Firebase の ML kit API を利用してスクショ画像内のテキストを取得する
3. Yahoo! JAPAN 日本簿形態素解析API を利用して、テキストを品詞ごとに区切る
4. 読みやすいように調整して、フラッシュ表示する

## Getting started

Speed Read では下記外部API・SDKを使用しているのでそれぞれ提供元に登録設定が必要です。

* [Firebase ML Kit](https://firebase.google.com/products/ml-kit/?hl=ja)
   * Firebase にプロジェクトを追加し、クラウドベースのMLキットを有効化する
   * https://firebase.google.com/docs/ml-kit/android/recognize-text?authuser=0
   * google-services.json を取得しておく
* [Yahoo! JAPAN 日本簿形態素解析API](https://developer.yahoo.co.jp/webapi/jlp/ma/v1/parse.html)
   * デベロッパー登録を行い、アプリケーションIDを取得しておく

* `res/values` 配下に `api_keys.xml` を作成し Yahoo!JAPAN で取得したアプリケーションIDを用いて下記の内容を貼り付ける

``` res/values/api_keys.xml

<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="yahoo_application_id">__COPY_HERE__</string>
</resources>

```

* google-services.json を `app` 配下に置く
* 完了。 run して試してみる。

## License

```
Copyright 2018 Teppei Tabuchi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```