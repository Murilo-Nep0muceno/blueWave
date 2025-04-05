

function nomeValid(){
    if(form.nomeVoluntario().length < 3  ){
        document.getElementById("error_name").style.display = "block "
        return false
    }else{
        document.getElementById("error_name").style.display = "none "
        return true
    }
}

function cpfValid(){
    if(validarCPF(form.cpf())){
        document.getElementById("error_cpf").style.display ="none"
        return true
    }else{
        document.getElementById("error_cpf").style.display ="block"
        return false
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

function validaData() {
    const dataInput = document.getElementById("dataNascimento").value;
    const dataNascimento = new Date(dataInput);
    const hoje = new Date();
  
    const dataLimite = new Date();
    dataLimite.setFullYear(hoje.getFullYear() - 18);
  
    if (dataNascimento > dataLimite) {
      document.getElementById("error_data").style.display = "block";
        return false
    } else {
      document.getElementById("error_data").style.display = "none";
      return true
    }
  }

function validarEmail(email) {

    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}


function validarCPF(cpf) {
    cpf = cpf.replace(/\D/g, ''); 

    if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) {
        return false; 
    }

    let soma = 0, resto;

    for (let i = 0; i < 9; i++) {
        soma += parseInt(cpf.charAt(i)) * (10 - i);
    }
    resto = (soma * 10) % 11;
    if (resto === 10 || resto === 11) resto = 0;
    if (resto !== parseInt(cpf.charAt(9))) return false;

    soma = 0;

    // Validação do segundo dígito verificador
    for (let i = 0; i < 10; i++) {
        soma += parseInt(cpf.charAt(i)) * (11 - i);
    }
    resto = (soma * 10) % 11;
    if (resto === 10 || resto === 11) resto = 0;
    if (resto !== parseInt(cpf.charAt(10))) return false;

    return true;
}

function validateForm(){
    const validNome = nomeValid();
    const validCPF = cpfValid();
    const validEmail = emailValid();
    const validSenha = senhaValid();
    const validSenhaConfirma = senhaConfirmaValid();
    const validTelefone = telefoneValid();
    const validData = validaData();
    
    return validNome && validCPF && validEmail && validSenha && validSenhaConfirma && validTelefone && validData;
}

async function form1(event){
    event.preventDefault();  // Impede o envio padrão do formulário

    if(!validateForm()){
        alert("Formulário invalido! ");
        return;
    } 


    const voluntario = {
        nomeVoluntario: form.nomeVoluntario(),
        email: form.email(),
        senha: form.senha(),
        cpf: form.cpf(),
        telefone: form.telefone(),
        dataNascimento: form.dataNascimento(),
        sexo: form.sexo()
    }

    try{
        const response = await fetch("http://localhost:8080/voluntarioForm", {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify(voluntario)
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
    cpf: ()=> document.querySelector("#cpf").value,
    email: ()=> document.querySelector("#email").value,
    senha: ()=> document.querySelector("#senha").value,
    confirmSenha: ()=> document.querySelector("#senhaConfirmar").value,
    telefone: ()=> document.querySelector("#telefone").value,
    dataNascimento: ()=>  document.getElementById("dataNascimento").value,
    sexo: ()=> document.querySelector("#sexo").value
}
