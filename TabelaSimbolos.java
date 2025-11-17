import java.util.ArrayList;
import java.util.List;

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

    public void setEndereco(int endereco) {
        this.endereco = endereco;
    }

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
    private int enderecoAtual;
    private int nivelAtual;

    public TabelaSimbolos() {
        tabela = new ArrayList<>();
        enderecoAtual = 0;
        nivelAtual = 0;
    }

    // ==========================
    // INSERÇÃO DE SÍMBOLOS
    // ==========================
    public boolean inserir(String nome, int escopo, String tipo) {
        System.out.println("Tentando inserir: " + nome + " no escopo " + escopo + " tipo: " + tipo);

        // Usa buscarNoNivelAtual para evitar redefinições no mesmo escopo
        if (buscarNoNivelAtual(nome) != null) {
            System.out.println("FALHA: " + nome + " já existe no escopo " + escopo);
            return false;
        }

        Simbolo simbolo = new Simbolo(nome, escopo, tipo, enderecoAtual++);
        tabela.add(simbolo);
        System.out.println("SUCESSO: " + nome + " inserido no escopo " + escopo);
        return true;
    }

    // ==========================
    // BUSCAS
    // ==========================

    // Busca o símbolo mais recente com determinado nome (de escopos internos para externos)
    public Simbolo buscar(String nome) {
        for (int i = tabela.size() - 1; i >= 0; i--) {
            Simbolo s = tabela.get(i);
            if (s.getNome().equals(nome)) {
                return s;
            }
        }
        return null;
    }

    // 🔹 Busca um símbolo apenas no escopo atual
    public Simbolo buscarNoNivelAtual(String nome) {
        for (int i = tabela.size() - 1; i >= 0; i--) {
            Simbolo s = tabela.get(i);
            if (s.getNome().equals(nome) && s.getEscopo() == nivelAtual) {
                return s; // encontrado no nível atual
            }
        }
        return null; // não encontrado neste nível
    }

    // ==========================
    // CONTROLE DE ESCOPO
    // ==========================

    public void entrarEscopo() {
        nivelAtual++;
        System.out.println("Entrando no escopo " + nivelAtual);
    }

    public void sairEscopo() {
        System.out.println("Saindo do escopo " + nivelAtual);
        // Remove símbolos pertencentes ao escopo atual
        tabela.removeIf(s -> s.getEscopo() == nivelAtual);
        nivelAtual--;
        if (nivelAtual < 0) nivelAtual = 0;
    }

    public int getNivelAtual() {
        return nivelAtual;
    }



    // ==========================
    // DEPURAÇÃO / UTILITÁRIOS
    // ==========================

    public void imprimir() {
        System.out.println("\n=== Tabela de Símbolos ===");
        for (Simbolo s : tabela) {
            System.out.println(s);
        }
        System.out.println("==========================\n");
    }

    public List<Simbolo> getTodos() {
        return new ArrayList<>(tabela); // retorna uma cópia segura
    }
}
