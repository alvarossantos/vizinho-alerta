package com.vizinhoalerta.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vizinhoalerta.data.AuthRepository
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ValidationTest {

    private val authRepository = AuthRepository()

    @Test
    fun senha_valida_tem_8_caracteres() {
        val senhaCurta = "Ab1!"
        val erro = authRepository.validarSenhaSegura(senhaCurta)
        assert(erro != null) { "Senha curta deveria falhar validação" }
    }

    @Test
    fun senha_valida_tem_maiuscula() {
        val senha = "abc123!@#"
        val erro = authRepository.validarSenhaSegura(senha)
        assert(erro != null) { "Senha sem maiuscula deveria falhar validação" }
    }

    @Test
    fun senha_valida_tem_numero() {
        val senha = "Abcd!@#"
        val erro = authRepository.validarSenhaSegura(senha)
        assert(erro != null) { "Senha sem numero deveria falhar validação" }
    }

    @Test
    fun senha_valida_tem_caracter_especial() {
        val senha = "Abc12345"
        val erro = authRepository.validarSenhaSegura(senha)
        assert(erro != null) { "Senha sem caractere especial deveria falhar validação" }
    }

    @Test
    fun senha_valida_passa_todas_validacoes() {
        val senha = "Senha123!"
        val erro = authRepository.validarSenhaSegura(senha)
        assert(erro == null) { "Senha valida deveria passar: $erro" }
    }
}