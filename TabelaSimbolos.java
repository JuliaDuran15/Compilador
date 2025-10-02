import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Simbolo {
    private String nome;
    private int escopo;
    private String tipo;
    private int endereco;

    public Simbolo(String nome, int escopo, String tipo, int endereco) {
        this.nome = nome;
        this.escopo = escopo;
        this.tipo = tipo;
        this.endereco = endereco;
    }

    public String getNome() { return nome; }
    public int getEscopo() { return escopo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getEndereco() { return endereco; }

    @Override
    public String toString() {
        return "Simbolo{" +
                "nome='" + nome + '\'' +
                ", escopo=" + escopo +
                ", tipo='" + tipo + '\'' +
                ", endereco=" + endereco +
                '}';
    }
}

public class TabelaSimbolos {
    private List<Simbolo> tabela;
    private Stack<String> identificadoresPendentes;
    private int enderecoAtual;
    private int nivelAtual;

    public TabelaSimbolos() {
        tabela = new ArrayList<>();
        identificadoresPendentes = new Stack<>();
        enderecoAtual = 0;
        nivelAtual = 0;
    }

    // Insere um símbolo na tabela
    public boolean inserir(String nome, int escopo, String tipo) {
        // Verifica se já existe símbolo com mesmo nome no mesmo escopo
        for (int i = tabela.size() - 1; i >= 0; i--) {
            Simbolo s = tabela.get(i);
            if (s.getNome().equals(nome) && s.getEscopo() == escopo) {
                return false; // já existe
            }
        }
        Simbolo simbolo = new Simbolo(nome, escopo, tipo, enderecoAtual++);
        tabela.add(simbolo);
        if (tipo.equals("variavel")) {
            identificadoresPendentes.push(nome);
        }
        return true;
    }

    // Busca um símbolo pelo nome (pega o mais recente em escopos internos)
    public Simbolo buscar(String nome) {
        for (int i = tabela.size() - 1; i >= 0; i--) {
            Simbolo s = tabela.get(i);
            if (s.getNome().equals(nome)) {
                return s;
            }
        }
        return null;
    }

    // Coloca tipo nas variáveis pendentes
    public void colocarTipoNasVariaveis(String tipo) {
        while (!identificadoresPendentes.isEmpty()) {
            String nome = identificadoresPendentes.pop();
            Simbolo s = buscar(nome);
            if (s != null && s.getTipo().equals("variavel")) {
                s.setTipo(tipo);
            }
        }
    }

    // Entra em novo escopo
    public void entrarEscopo() {
        nivelAtual++;
    }

    // Sai do escopo atual, removendo símbolos do nível
    public void sairEscopo() {
        for (int i = tabela.size() - 1; i >= 0; i--) {
            Simbolo s = tabela.get(i);
            if (s.getEscopo() == nivelAtual) {
                tabela.remove(i);
            }
        }
        nivelAtual--;
    }

    // Retorna o nível atual do escopo
    public int getNivelAtual() {
        return nivelAtual;
    }

    // Imprime a tabela de símbolos
    public void imprimir() {
        System.out.println("=== Tabela de Simbolos ===");
        for (Simbolo s : tabela) {
            System.out.println(s);
        }
    }
}
