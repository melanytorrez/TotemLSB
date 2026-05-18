package com.ucb.app.di

import com.ucb.app.totem.domain.repository.CareerRepository
import org.koin.dsl.module

val dataModule = module {
    single { CareerRepository() }
}