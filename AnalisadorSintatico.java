import java.io.IOException;

public class AnalisadorSintatico {
    private Token tokenAtual;
    private TabelaSimbolos tabela;
    private AnalisadorLexico lexico;
    private int nivelAtual = 0;

    public AnalisadorSintatico(AnalisadorLexico lexico, TabelaSimbolos tabela) throws IOException {
        this.lexico = lexico;
        this.tabela = tabela;
        proximoToken();
    }

    private void proximoToken() throws IOException {
        tokenAtual = lexico.pegaToken();
    }

    private void erro(String msg) {
        if (tokenAtual != null) {
            throw new RuntimeException("Erro sintático na linha " + tokenAtual.getLinha() + ": " + msg);
        } else {
            throw new RuntimeException("Erro sintático: " + msg + " (Fim inesperado do arquivo)");
        }
    }

    public void analisaPrograma() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.sprograma) {
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
                tabela.inserir(tokenAtual.getLexema(), nivelAtual, "programa");
                proximoToken();
                if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                    proximoToken();
                    analisaBloco();
                    if (tokenAtual.getSimbolo() == TokenSimbolo.sponto) {
                        proximoToken();
                        System.out.println("Programa válido!");
                    } else {
                        erro("Ponto final esperado");
                    }
                } else {
                    erro("Ponto e vírgula esperado após identificador");
                }
            } else {
                erro("Identificador esperado após 'programa'");
            }
        } else {
            erro("Palavra-chave 'programa' esperada");
        }
    }

    private void analisaBloco() throws IOException {
        nivelAtual++;
        tabela.entrarEscopo();

        analisaEtVariaveis();
        analisaSubrotinas();
        analisaComandos();

        tabela.sairEscopo();
        nivelAtual--;
    }

    private void analisaEtVariaveis() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.svar) {
            proximoToken();
            while (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
                analisaVariaveis();
                if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                    proximoToken();
                } else {
                    erro("Ponto e vírgula esperado após declaração de variáveis");
                }
            }
        }
    }

    private void analisaVariaveis() throws IOException {
        // Analisa o primeiro identificador
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "variavel")) {
                erro("Identificador '" + tokenAtual.getLexema() + "' já declarado");
            }
            proximoToken();
        } else {
            erro("Identificador esperado na declaração de variáveis");
        }
        
        // Analisa identificadores subsequentes separados por vírgula
        while (tokenAtual.getSimbolo() == TokenSimbolo.svirgula) {
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
                if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "variavel")) {
                    erro("Identificador '" + tokenAtual.getLexema() + "' já declarado");
                }
                proximoToken();
            } else {
                erro("Identificador esperado após vírgula na declaração de variáveis");
            }
        }

        // Espera os dois pontos e o tipo
        if (tokenAtual.getSimbolo() == TokenSimbolo.sdois_pontos) {
            proximoToken();
            analisaTipo();
        } else {
            erro("Dois pontos esperado em declaração de variáveis");
        }
    }

    private void analisaTipo() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.sinteiro || tokenAtual.getSimbolo() == TokenSimbolo.sbooleano) {
            tabela.colocarTipoNasVariaveis(tokenAtual.getLexema());
            proximoToken();
        } else {
            erro("Tipo esperado (inteiro ou booleano)");
        }
    }

private void analisaComandos() throws IOException {
    if (tokenAtual.getSimbolo() == TokenSimbolo.sinicio) {
        proximoToken();
        analisaComandoSimples();
        while (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
            proximoToken();
            if (tokenAtual.getSimbolo() != TokenSimbolo.sfim) {
                analisaComandoSimples();
            }
        }
        if (tokenAtual.getSimbolo() == TokenSimbolo.sfim) {
            proximoToken();
        } else {
            erro("'fim' esperado para finalizar o bloco de comandos");
        }
    } else {
        erro("'inicio' esperado no bloco de comandos");
    }
}

    private void analisaComandoSimples() throws IOException {
    if (tokenAtual.getSimbolo() == TokenSimbolo.sleia) {
        analisaLeia();
    } else if (tokenAtual.getSimbolo() == TokenSimbolo.sescreva) {
        analisaEscreva();
    } else if (tokenAtual.getSimbolo() == TokenSimbolo.sse) {
        analisaSe();
    } else if (tokenAtual.getSimbolo() == TokenSimbolo.senquanto) {
        analisaEnquanto();
    } else if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
        analisaAtribChProcedimento();
    } else if (tokenAtual.getSimbolo() == TokenSimbolo.sinicio) {
        analisaComandos(); // <--- Este é o ponto chave para aninhamento
    } else {
                erro("Comando inválido");
        }
    }

private void analisaAtribChProcedimento() throws IOException {
    if (tabela.buscar(tokenAtual.getLexema()) == null) {
        erro("Identificador '" + tokenAtual.getLexema() + "' não declarado");
    }
    proximoToken();
    if (tokenAtual.getSimbolo() == TokenSimbolo.satribuicao) {
        proximoToken();
        analisaExpressao(); // Chama a análise da expressão para a atribuição
    } else {
        // Se não for ':=', a atribuição falha.
        // O seu código já lança o erro, mas de forma genérica.
        // Você pode ser mais específico:
        erro("Símbolo de atribuição ':=', ou chamada de procedimento, esperado");
    }
}

    private void analisaLeia() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sabre_parenteses) {
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
                if (tabela.buscar(tokenAtual.getLexema()) != null) {
                    proximoToken();
                    if (tokenAtual.getSimbolo() == TokenSimbolo.sfecha_parenteses) {
                        proximoToken();
                    } else {
                        erro("Parêntese fechando esperado");
                    }
                } else {
                    erro("Identificador '" + tokenAtual.getLexema() + "' não declarado");
                }
            } else {
                erro("Identificador esperado na instrução 'leia'");
            }
        } else {
            erro("Parêntese abrindo esperado na instrução 'leia'");
        }
    }

    private void analisaEscreva() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sabre_parenteses) {
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
                if (tabela.buscar(tokenAtual.getLexema()) != null) {
                    proximoToken();
                    if (tokenAtual.getSimbolo() == TokenSimbolo.sfecha_parenteses) {
                        proximoToken();
                    } else {
                        erro("Parêntese fechando esperado");
                    }
                } else {
                    erro("Identificador '" + tokenAtual.getLexema() + "' não declarado");
                }
            } else {
                erro("Identificador esperado na instrução 'escreva'");
            }
        } else {
            erro("Parêntese abrindo esperado na instrução 'escreva'");
        }
    }

    private void analisaEnquanto() throws IOException {
        proximoToken();
        analisaExpressao();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sfaca) {
            proximoToken();
            analisaComandoSimples();
        } else {
            erro("'faca' esperado na instrução 'enquanto'");
        }
    }

    private void analisaSe() throws IOException {
        proximoToken();
        analisaExpressao();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sentao) {
            proximoToken();
            analisaComandoSimples();
            if (tokenAtual.getSimbolo() == TokenSimbolo.ssenao) {
                proximoToken();
                analisaComandoSimples();
            }
        } else {
            erro("'entao' esperado na instrução 'se'");
        }
    }

    private void analisaSubrotinas() throws IOException {
        while (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento || tokenAtual.getSimbolo() == TokenSimbolo.sfuncao) {
            if (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento) {
                analisaDeclaracaoProcedimento();
            } else {
                analisaDeclaracaoFuncao();
            }
            if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                proximoToken();
            } else {
                erro("Ponto e vírgula esperado após declaração de sub-rotina");
            }
        }
    }

    private void analisaDeclaracaoProcedimento() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "procedimento")) {
                erro("Procedimento '" + tokenAtual.getLexema() + "' já declarado");
            }
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                proximoToken();
                analisaBloco();
            } else {
                erro("Ponto e vírgula esperado após identificador de procedimento");
            }
        } else {
            erro("Identificador esperado após 'procedimento'");
        }
    }

    private void analisaDeclaracaoFuncao() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "funcao")) {
                erro("Função '" + tokenAtual.getLexema() + "' já declarada");
            }
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sdois_pontos) {
                proximoToken();
                if (tokenAtual.getSimbolo() == TokenSimbolo.sinteiro || tokenAtual.getSimbolo() == TokenSimbolo.sbooleano) {
                    proximoToken();
                    if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                        proximoToken();
                        analisaBloco();
                    } else {
                        erro("Ponto e vírgula esperado após tipo de função");
                    }
                } else {
                    erro("Tipo esperado (inteiro ou booleano) após ':' na função");
                }
            } else {
                erro("Dois pontos esperado após identificador de função");
            }
        } else {
            erro("Identificador esperado após 'funcao'");
        }
    }

    private void analisaExpressao() throws IOException {
        analisaExpressaoSimples();
        if (tokenAtual.getSimbolo() == TokenSimbolo.smaior || tokenAtual.getSimbolo() == TokenSimbolo.smaior_ig ||
            tokenAtual.getSimbolo() == TokenSimbolo.sigual || tokenAtual.getSimbolo() == TokenSimbolo.smenor ||
            tokenAtual.getSimbolo() == TokenSimbolo.smenor_ig || tokenAtual.getSimbolo() == TokenSimbolo.sdiferente) {
            proximoToken();
            analisaExpressaoSimples();
        }
    }

    private void analisaExpressaoSimples() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.smais || tokenAtual.getSimbolo() == TokenSimbolo.smenos) {
            proximoToken();
        }
        analisaTermo();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smais || tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
               tokenAtual.getSimbolo() == TokenSimbolo.sou) {
            proximoToken();
            analisaTermo();
        }
    }

    private void analisaTermo() throws IOException {
        analisaFator();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smultiplicacao || tokenAtual.getSimbolo() == TokenSimbolo.sdiv ||
               tokenAtual.getSimbolo() == TokenSimbolo.se) {
            proximoToken();
            analisaFator();
        }
    }

    private void analisaFator() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (tabela.buscar(tokenAtual.getLexema()) != null) {
                proximoToken();
            } else {
                erro("Identificador '" + tokenAtual.getLexema() + "' não declarado");
            }
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.snumero) {
            proximoToken();
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.snao) {
            proximoToken();
            analisaFator();
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.sabre_parenteses) {
            proximoToken();
            analisaExpressao();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sfecha_parenteses) {
                proximoToken();
            } else {
                erro("Parêntese fechando esperado");
            }
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.sverdadeiro || tokenAtual.getSimbolo() == TokenSimbolo.sfalso) {
            proximoToken();
        } else {
            erro("Fator inválido");
        }
    }
}
