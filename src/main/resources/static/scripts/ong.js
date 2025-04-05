

function nomeValid(){
    if(form.nomeVoluntario().length < 3  ){
        document.getElementById("error_name").style.display = "block "
        return false
    }else{
        document.getElementById("error_name").style.display = "none "
        return true
    }
}

function cnpjValid() {
    const cnpjInput = document.querySelector("#cnpj").value;
    
    if (validarCNPJ(cnpjInput)) {
      document.getElementById("error_cnpj").style.display = "none";
      return true;
    } else {
      document.getElementById("error_cnpj").style.display = "block";
      return false;
    }
  }

function emailValid(){
    if(validarEmail(form.email())){
        document.getElementById("error_email").style.display ="none"
        return true
    }else{
        document.getElementById("error_email").style.display ="block"
        return false
    }
}

function senhaValid(){
    if(form.senha().length < 3 ){
        document.getElementById("error_senha").style.display ="block"
        return false
    }else{
        document.getElementById("error_senha").style.display ="none"
        return true
    }

}

function senhaConfirmaValid(){
    if(form.confirmSenha() === form.senha()){
        document.getElementById("error_senhaConfirmar").style.display ="none"
        return true
    }else{
        document.getElementById("error_senhaConfirmar").style.display ="block"
        return false
        
    }
}

function telefoneValid(){
    if(form.telefone().length === 11  ){
        document.getElementById("error_telefone").style.display ="none"
        return true
    }else{
        document.getElementById("error_telefone").style.display ="block"
        return false

    }
}


function validarCEP(cep) {
    const cepApenasNumeros = cep.replace(/\D/g, '');
    return /^[0-9]{8}$/.test(cepApenasNumeros);
  }
  
  function cepValid() {
    
    if (validarCEP(form.cep())) {
      document.getElementById("error_cep").style.display = "none";
      return true;
    } else {
      document.getElementById("error_cep").style.display = "block";
      return false;
    }
  }



function validarEmail(email) {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

function validarCNPJ(cnpj) {
    cnpj = cnpj.replace(/[^\d]+/g, '');
  
    if (cnpj === '') return false;
    if (cnpj.length !== 14) return false;
    if (/^(\d)\1+$/.test(cnpj)) return false; 
    let tamanho = cnpj.length - 2;
    let numeros = cnpj.substring(0, tamanho);
    let digitos = cnpj.substring(tamanho);
    let soma = 0;
    let pos = tamanho - 7;
    
    for (let i = tamanho; i >= 1; i--) {
      soma += numeros.charAt(tamanho - i) * pos--;
      if (pos < 2) pos = 9;
    }
    
    let resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    if (resultado !== parseInt(digitos.charAt(0))) return false;
    
    tamanho = tamanho + 1;
    numeros = cnpj.substring(0, tamanho);
    soma = 0;
    pos = tamanho - 7;
    
    for (let i = tamanho; i >= 1; i--) {
      soma += numeros.charAt(tamanho - i) * pos--;
      if (pos < 2) pos = 9;
    }
    
    resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    if (resultado !== parseInt(digitos.charAt(1))) return false;
    
    return true;
  }



function validateForm(){
    const validNome = nomeValid();
    const validarCNPJ = cnpjValid();
    const validEmail = emailValid();
    const validSenha = senhaValid();
    const validSenhaConfirma = senhaConfirmaValid();
    const validTelefone = telefoneValid();
    
    return validNome && validEmail && validSenha && validSenhaConfirma && validTelefone && validarCNPJ ;
}


async function form2(event){
    event.preventDefault();  // Impede o envio padrão do formulário

    if(!validateForm()){
        alert("Formulário invalido! ");
        return;
    } 


    const ong = {
        nome: form.nomeVoluntario(),
        email: form.email(),
        senha: form.senha(),
        cnpj: form.cnpj().replace(/\D/g, ""),
        telefone: form.telefone(),
        cep: form.cep()
    }

    try{
        const response = await fetch("http://localhost:8080/ongForm", {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify(ong)
          });

          if (response.ok) {
            alert("Formulário enviado com sucesso!");
            // Pode redirecionar, limpar o form, etc.
          } else {
            const errorData = await response.json();
            alert("Erro ao enviar: " + (errorData.message || "Erro desconhecido"));
          }
        

    }catch(error){
        console.error("error ao enviar", error)
    }

}


const form = {
    nomeVoluntario: ()=> document.querySelector("#nomeVoluntario").value,
    cnpj: ()=> document.querySelector("#cnpj").value,
    email: ()=> document.querySelector("#email").value,
    senha: ()=> document.querySelector("#senha").value,
    confirmSenha: ()=> document.querySelector("#senhaConfirmar").value,
    telefone: ()=> document.querySelector("#telefone").value,
    cep: ()=> document.querySelector("#cep").value
}
