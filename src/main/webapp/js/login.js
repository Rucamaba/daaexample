function showError() {
    const errorDiv = document.getElementById('login-error');
    errorDiv.classList.remove('d-none');
    errorDiv.classList.add('show');
}

function hideError() {
    const errorDiv = document.getElementById('login-error');
    errorDiv.classList.remove('show');
    errorDiv.classList.add('d-none');
}

function doLogin(login, password) {
    // Hide any previous error message
    hideError();
    
    $.ajax({
        url: 'rest/users/' + login,
        type: 'GET',
        headers: {
            // Send credentials in a custom header instead of using Authorization
            'Custom-Auth': 'Basic ' + btoa(login + ":" + password)
        }
    })
    .done(function(user) {
        localStorage.setItem('authorization-token', btoa(login + ":" + password));
        localStorage.setItem('user-role', user.role);
        window.location = 'main.html';
    })
    .fail(function() {
        showError();
        // Clear the password field for security
        document.getElementById('password').value = '';
        document.getElementById('password').focus();
    });
}

function doLogout() {
    localStorage.removeItem('authorization-token');
    window.location = 'index.html';
}