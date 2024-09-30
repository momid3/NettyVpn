package com.momid

import com.momid.screening.startScreeningClient

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        startServer()
    } else {
        if (args.size == 1) {
            if (args[0] == "screening") {
                startScreeningClient()
            }
        }
    }
}
