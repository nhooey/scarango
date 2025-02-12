package com.outr.arango.query.dsl

import com.outr.arango.query._

class Filter(left: Query, condition: String, right: Query) {
  def &&(filter: Filter): Filter = {
    new Filter(build(), "&&", filter.build())
  }
  def ||(filter: Filter): Filter = {
    new Filter(build(), "||", filter.build())
  }

  def build(): Query = Query(
    left, s" $condition ", right
  )
}
