var TypesDAO = (function() {
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

    function TypesDAO() {
        this.listTypes = function(success, error) {
            requestByAjax({
                url: 'rest/types',
                type: 'GET',
                dataType: 'json',
                error: function(jqxhr, textStatus, errorThrown) {
                    console.error('Request error:', textStatus, errorThrown);
                    if (error) error(textStatus + ': ' + errorThrown);
                }
            }, success);
        };

        this.getType = function(id, success, error) {
            requestByAjax({
                url: 'rest/types/' + id,
                type: 'GET',
                dataType: 'json',
                error: function(jqxhr, textStatus, errorThrown) {
                    if (error) error(textStatus + ': ' + errorThrown);
                }
            }, success);
        };

        this.addType = function(type, success, error) {
            requestByAjax({
                url: 'rest/types',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(type),
                error: function(jqxhr, textStatus, errorThrown) {
                    if (error) error(textStatus + ': ' + errorThrown);
                }
            }, success);
        };
    }

    return TypesDAO;
})();