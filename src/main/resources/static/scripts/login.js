document.getElementById("loginForm").addEventListener("submit", async function (event) {
    event.preventDefault(); 
    const email = document.getElementById("email").value;
    const senha = document.getElementById("senha").value;
    const tipo = document.getElementById("tipo").value;

    const dados = {
      email: email,
      senha: senha,
      tipo: tipo
    };

    try {
      const response = await fetch("http://localhost:8080/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(dados)
      });

      if (response.ok) {
        const result = await response.json(); 

      } else {
        alert("Erro ao fazer login!");
      }
    } catch (error) {
      console.error("Erro de conexão:", error);
      alert("Falha na requisição!");
    }
  });