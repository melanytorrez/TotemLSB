package com.ucb.app.di

import com.ucb.app.totem.presentation.viewmodel.TotemViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::TotemViewModel)
}