var PetsDAO = (function() {
    var resourcePath = "rest/people/";

    var requestByAjax = function(data, done, fail, always) {
        done = typeof done !== 'undefined' ? done : function() {};
        fail = typeof fail !== 'undefined' ? fail : function() {};
        always = typeof always !== 'undefined' ? always : function() {};

        let authToken = localStorage.getItem('authorization-token');
        if (authToken !== null) {
            data.beforeSend = function(xhr) {
                xhr.setRequestHeader('Custom-Auth', 'Basic ' + authToken);
            };
        }

        $.ajax(data).done(done).fail(fail).always(always);
    };

    function PetsDAO() {
        this.listPets = function(personId, done, fail, always) {
            requestByAjax({
                url: resourcePath + personId + "/pets",
                type: 'GET'
            }, done, fail, always);
        };

        this.addPet = function(personId, pet, done, fail, always) {
            requestByAjax({
                url: resourcePath + personId + "/pets",
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(pet)
            }, done, fail, always);
        };

        this.modifyPet = function(personId, pet, done, fail, always) {
            requestByAjax({
                url: resourcePath + personId + "/pets/" + pet.id,
                type: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify(pet)
            }, done, fail, always);
        };

        this.deletePet = function(personId, petId, done, fail, always) {
            requestByAjax({
                url: resourcePath + personId + "/pets/" + petId,
                type: 'DELETE'
            }, done, fail, always);
        };
    }

    return PetsDAO;
})();