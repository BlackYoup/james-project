/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.core

import java.net.{URI, URL}
import java.util.Optional

import org.apache.commons.configuration2.Configuration
import org.apache.james.jmap.core.JmapRfc8621Configuration.UPLOAD_LIMIT_30_MB
import org.apache.james.jmap.pushsubscription.PushClientConfiguration
import org.apache.james.util.Size

import scala.jdk.OptionConverters._

object JmapRfc8621Configuration {
  val LOCALHOST_URL_PREFIX: String = "http://localhost"
  val LOCALHOST_WEBSOCKET_URL_PREFIX: String = "ws://localhost"
  val UPLOAD_LIMIT_30_MB: MaxSizeUpload = MaxSizeUpload.of(Size.of(30L, Size.Unit.M)).get
  val LOCALHOST_CONFIGURATION: JmapRfc8621Configuration = JmapRfc8621Configuration(LOCALHOST_URL_PREFIX, LOCALHOST_WEBSOCKET_URL_PREFIX, UPLOAD_LIMIT_30_MB)
  val URL_PREFIX_PROPERTIES: String = "url.prefix"
  val WEBSOCKET_URL_PREFIX_PROPERTIES: String = "websocket.url.prefix"
  val UPLOAD_LIMIT_PROPERTIES: String = "upload.max.size"

  def from(configuration: Configuration): JmapRfc8621Configuration = {
    JmapRfc8621Configuration(
      urlPrefixString = Option(configuration.getString(URL_PREFIX_PROPERTIES)).getOrElse(LOCALHOST_URL_PREFIX),
      websocketPrefixString = Option(configuration.getString(WEBSOCKET_URL_PREFIX_PROPERTIES)).getOrElse(LOCALHOST_WEBSOCKET_URL_PREFIX),
      maxUploadSize = Option(configuration.getString(UPLOAD_LIMIT_PROPERTIES, null))
        .map(Size.parse)
        .map(MaxSizeUpload.of(_).get)
        .getOrElse(UPLOAD_LIMIT_30_MB),
      maxTimeoutSeconds = Optional.ofNullable(configuration.getInteger("webpush.maxTimeoutSeconds", null)).map(Integer2int).toScala,
      maxConnections = Optional.ofNullable(configuration.getInteger("webpush.maxConnections", null)).map(Integer2int).toScala)
  }
}

case class JmapRfc8621Configuration(urlPrefixString: String, websocketPrefixString: String,
                                    maxUploadSize: MaxSizeUpload = UPLOAD_LIMIT_30_MB,
                                    maxTimeoutSeconds: Option[Int] = None,
                                    maxConnections: Option[Int] = None) {
  val urlPrefix: URL = new URL(urlPrefixString)
  val apiUrl: URL = new URL(s"$urlPrefixString/jmap")
  val downloadUrl: URL = new URL(urlPrefixString + "/download/{accountId}/{blobId}?type={type}&name={name}")
  val uploadUrl: URL = new URL(s"$urlPrefixString/upload/{accountId}")
  val eventSourceUrl: URL = new URL(s"$urlPrefixString/eventSource?types={types}&closeAfter={closeafter}&ping={ping}")
  val webSocketUrl: URI = new URI(s"$websocketPrefixString/jmap/ws")

  val webPushConfiguration: PushClientConfiguration = PushClientConfiguration(
    maxTimeoutSeconds = maxTimeoutSeconds,
    maxConnections = maxConnections)
}
