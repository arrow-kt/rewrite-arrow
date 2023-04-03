package org.example

import arrow.core.Either
import arrow.core.continuations.Effect
import arrow.core.continuations.either
import arrow.core.continuations.effect

fun example2(): Either<String, Int> = either.eager {
  ensure(false) { "failure" }
  1
}

val x: Effect<String, Int> = effect {
  3
}
