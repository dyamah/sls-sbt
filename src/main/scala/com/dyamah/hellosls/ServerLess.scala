package com.dyamah.hellosls

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
case class Provider(stage: String)

@JsonIgnoreProperties(ignoreUnknown=true)
case class ServerLess(
                     service: String,
                     provider: Provider
                     )

