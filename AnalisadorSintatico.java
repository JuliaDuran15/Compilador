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
            throw new RuntimeException("Erro sintatico na linha " + tokenAtual.getLinha() + ": " + msg);
        } else {
            throw new RuntimeException("Erro sintatico: " + msg + " (Fim inesperado do arquivo)");
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
                        System.out.println("Programa valido!");
                    } else {
                        erro("Ponto final esperado");
                    }
                } else {
                    erro("Ponto e virgula esperado apos identificador");
                }
            } else {
                erro("Identificador esperado apos 'programa'");
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
                    erro("Ponto e virgula esperado apos declaracao de variaveis");
                }
            }
        }
    }

    private void analisaVariaveis() throws IOException {
        // Analisa o primeiro identificador
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "variavel")) {
                erro("Identificador '" + tokenAtual.getLexema() + "' ja declarado");
            }
            proximoToken();
        } else {
            erro("Identificador esperado na declaracao de variaveis");
        }
        
        // Analisa identificadores subsequentes separados por vírgula
        while (tokenAtual.getSimbolo() == TokenSimbolo.svirgula) {
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sdois_pontos) {
                erro("Virgula a mais na declaracao de variaveis");
            }
            if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
                if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "variavel")) {
                    erro("Identificador '" + tokenAtual.getLexema() + "' já declarado");
                }
                proximoToken();
            } else {
                erro("Identificador esperado apos virgula na declaracao de variaveis");
            }
        }

        // Espera os dois pontos e o tipo
        if (tokenAtual.getSimbolo() == TokenSimbolo.sdois_pontos) {
            proximoToken();
            analisaTipo();
        } else {
            erro("Dois pontos esperado em declaracao de variaveis");
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
            if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula || tokenAtual.getSimbolo() == TokenSimbolo.ssenao) {
                erro("Ponto e virgula excedente");
            }
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
                erro("Comando invalido");
        }
    }

private void analisaAtribChProcedimento() throws IOException {
    if (this.tabela.buscar(this.tokenAtual.getLexema()) == null) {
        this.erro("Identificador '" + this.tokenAtual.getLexema() + "' nao declarado");
    }

    this.proximoToken();
    if (this.tokenAtual.getSimbolo() == TokenSimbolo.satribuicao) {
        // caso de atribuição
        this.proximoToken();
        this.analisaExpressao();
    } else if (this.tokenAtual.getSimbolo() == TokenSimbolo.sabre_parenteses) {
        // chamada de procedimento com parâmetros
        this.proximoToken();
        this.analisaExpressao();
        while (this.tokenAtual.getSimbolo() == TokenSimbolo.svirgula) {
            this.proximoToken();
            this.analisaExpressao();
        }
        if (this.tokenAtual.getSimbolo() == TokenSimbolo.sfecha_parenteses) {
            this.proximoToken();
        } else {
            this.erro("Parentese fechando esperado na chamada de procedimento");
        }
    } else {
        // chamada de procedimento simples (sem parâmetros)
        // não precisa fazer nada além de aceitar e retornar
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
                        erro("Parentese fechando esperado");
                    }
                } else {
                    erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");
                }
            } else {
                erro("Identificador esperado na instrucao 'leia'");
            }
        } else {
            erro("Parentese abrindo esperado na instrucao 'leia'");
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
                        erro("Parentese fechando esperado");
                    }
                } else {
                    erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");
                }
            } else {
                erro("Identificador esperado na instrucao 'escreva'");
            }
        } else {
            erro("Parentese abrindo esperado na instrucao 'escreva'");
        }
    }

    private void analisaEnquanto() throws IOException {
        proximoToken();
        analisaExpressao();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sfaca) {
            proximoToken();
            analisaComandoSimples();
        } else {
            erro("'faca' esperado na instrucao 'enquanto'");
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
            erro("'entao' esperado na instrucao 'se'");
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
                erro("Ponto e virgula esperado apos declaracao de sub-rotina");
            }
        }
    }

    private void analisaDeclaracaoProcedimento() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "procedimento")) {
                erro("Procedimento '" + tokenAtual.getLexema() + "' ja declarado");
            }
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                proximoToken();
                analisaBloco();
            } else {
                erro("Ponto e virgula esperado apos identificador de procedimento");
            }
        } else {
            erro("Identificador esperado apos 'procedimento'");
        }
    }

    private void analisaDeclaracaoFuncao() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (!tabela.inserir(tokenAtual.getLexema(), nivelAtual, "funcao")) {
                erro("Funcao '" + tokenAtual.getLexema() + "' ja declarada");
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
                        erro("Ponto e virgula esperado apos tipo de funcao");
                    }
                } else {
                    erro("Tipo esperado (inteiro ou booleano) apos ':' na função");
                }
            } else {
                erro("Dois pontos esperado apos identificador de função");
            }
        } else {
            erro("Identificador esperado apos 'funcao'");
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
                erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");
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
                erro("Parentese fechando esperado");
            }
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.sverdadeiro || tokenAtual.getSimbolo() == TokenSimbolo.sfalso) {
            proximoToken();
        } else {
            erro("Fator invalido");
        }
    }
}
