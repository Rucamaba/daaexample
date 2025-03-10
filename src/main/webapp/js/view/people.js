var PeopleView = (function() {
    var dao;
    
    // Referencia a this que permite acceder a las funciones públicas desde las funciones de jQuery.
    var self;
    
    var formId = 'people-form';
    var listId = 'people-list';
    var formQuery = '#' + formId;
    var listQuery = '#' + listId;
    
    // Store PetsView instances by person ID
    var petsViews = {};
    
    function PeopleView(peopleDao, formContainerId, listContainerId) {
        dao = peopleDao;
        self = this;
        this.petsDao = new PetsDAO();
        this.typesDao = new TypesDAO();
        
        const userRole = localStorage.getItem('user-role');
        const isAdmin = userRole === 'ADMIN';
        
        if (isAdmin) {
            insertPeopleForm($('#' + formContainerId));
        }
        insertPeopleList($('#' + listContainerId));
        
        this.init = function() {
            dao.listPeople(function(people) {
                $.each(people, function(key, person) {
                    appendToTable(person);
                });
            },
            function() {
                alert('No has sido posible acceder al listado de personas.');
            });
            
            // La acción por defecto de enviar formulario (submit) se sobreescribe
            // para que el envío sea a través de AJAX
            $(formQuery).submit(function(event) {
                var person = self.getPersonInForm();
                
                if (self.isEditing()) {
                    dao.modifyPerson(person,
                        function(person) {
                            $('#person-' + person.id + ' td.name').text(person.name);
                            $('#person-' + person.id + ' td.surname').text(person.surname);
                            self.resetForm();
                        },
                        showErrorMessage,
                        self.enableForm
                    );
                } else {
                    dao.addPerson(person,
                        function(person) {
                            appendToTable(person);
                            self.resetForm();
                        },
                        showErrorMessage,
                        self.enableForm
                    );
                }
                
                return false;
            });
            
            $('#btnClear').click(this.resetForm);
        };

        this.getPersonInForm = function() {
            var form = $(formQuery);
            return {
                'id': form.find('input[name="id"]').val(),
                'name': form.find('input[name="name"]').val(),
                'surname': form.find('input[name="surname"]').val()
            };
        };

        this.getPersonInRow = function(id) {
            var row = $('#person-' + id);

            if (row !== undefined) {
                return {
                    'id': id,
                    'name': row.find('td.name').text(),
                    'surname': row.find('td.surname').text()
                };
            } else {
                return undefined;
            }
        };
        
        this.editPerson = function(id) {
            var row = $('#person-' + id);

            if (row !== undefined) {
                var form = $(formQuery);
                
                form.find('input[name="id"]').val(id);
                form.find('input[name="name"]').val(row.find('td.name').text());
                form.find('input[name="surname"]').val(row.find('td.surname').text());
                
                $('input#btnSubmit').val('Modificar');
            }
        };
        
        this.deletePerson = function(id) {
            if (confirm('Está a punto de eliminar a una persona. ¿Está seguro de que desea continuar?')) {
                dao.deletePerson(id,
                    function() {
                        // Cleanup PetsView if it exists
                        if (petsViews[id]) {
                            petsViews[id].cleanup();
                            delete petsViews[id];
                        }
                        // Remove both the person row and the pets list row
                        var $personRow = $('tr#person-' + id);
                        var $petsListRow = $personRow.next('.pets-list');
                        $petsListRow.remove();
                        $personRow.remove();
                    },
                    showErrorMessage
                );
            }
        };

        this.isEditing = function() {
            return $(formQuery + ' input[name="id"]').val() != "";
        };

        this.disableForm = function() {
            $(formQuery + ' input').prop('disabled', true);
        };

        this.enableForm = function() {
            $(formQuery + ' input').prop('disabled', false);
        };

        this.resetForm = function() {
            $(formQuery)[0].reset();
            $(formQuery + ' input[name="id"]').val('');
            $('#btnSubmit').val('Crear');
        };
    };
    
    var insertPeopleList = function(parent) {
        parent.append(
            '<table id="' + listId + '" class="table">\
                <thead>\
                    <tr class="row">\
                        <th class="col-sm-3">Nombre</th>\
                        <th class="col-sm-4">Apellido</th>\
                        <th class="col-sm-5">Acciones</th>\
                    </tr>\
                </thead>\
                <tbody>\
                </tbody>\
            </table>'
        );
    };

    var insertPeopleForm = function(parent) {
        parent.append(
            '<form id="' + formId + '" class="mb-5 mb-10">\
                <input name="id" type="hidden" value=""/>\
                <div class="row">\
                    <div class="col-sm-4">\
                        <input name="name" type="text" value="" placeholder="Nombre" class="form-control" required/>\
                    </div>\
                    <div class="col-sm-5">\
                        <input name="surname" type="text" value="" placeholder="Apellido" class="form-control" required/>\
                    </div>\
                    <div class="col-sm-3">\
                        <input id="btnSubmit" type="submit" value="Crear" class="btn btn-primary" />\
                        <input id="btnClear" type="reset" value="Limpiar" class="btn" />\
                    </div>\
                </div>\
            </form>'
        );
    };

    var createPersonRow = function(person) {
        const userRole = localStorage.getItem('user-role');
        const isAdmin = userRole === 'ADMIN';
        
        var actionButtons = '<a class="view-pets btn btn-success btn-sm" href="#">Ver Mascotas</a>';
        if (isAdmin) {
            actionButtons =
                '<a class="edit btn btn-primary btn-sm me-1" href="#">Editar</a>' +
                '<a class="delete btn btn-warning btn-sm me-1" href="#">Eliminar</a>' +
                actionButtons;
        }
        
        var row = $('<tr id="person-'+ person.id +'" class="row">\
            <td class="name col-sm-3">' + person.name + '</td>\
            <td class="surname col-sm-4">' + person.surname + '</td>\
            <td class="col-sm-5">' + actionButtons + '</td>\
        </tr>');
        
        return row[0].outerHTML;
    };

    var showErrorMessage = function(jqxhr, textStatus, error) {
        alert(textStatus + ": " + error);
    };

    var addRowListeners = function(person) {
        $('#person-' + person.id + ' a.edit').click(function() {
            self.editPerson(person.id);
        });
        
        $('#person-' + person.id + ' a.delete').click(function() {
            self.deletePerson(person.id);
        });

        $('#person-' + person.id + ' a.view-pets').on('click', function(event) {
            event.preventDefault();
            console.log('View pets clicked for person:', person);
            
            var personRow = $('#person-' + person.id);
            var existingPetsList = personRow.next('.pets-list');
            
            if (existingPetsList.length) {
                existingPetsList.remove();
                // Clean up PetsView when closing the pets list
                if (petsViews[person.id]) {
                    petsViews[person.id].cleanup();
                    delete petsViews[person.id];
                }
                return;
            }

            try {
                self.petsDao.listPets(person.id,
                    function(pets) {
                        console.log('Pets received:', pets);
                        var petsList = '<tr class="pets-list">\
                            <td colspan="3" class="p-3">\
                                <div class="ms-4">\
                                    <h6>Mascotas de ' + person.name + ':</h6>\
                                    <div id="pets-list-container-' + person.id + '">\
                                        <ul class="list-group">';
                        
                        if (!pets || pets.length === 0) {
                            petsList += '<li class="list-group-item">No tiene mascotas</li>';
                        } else {
                            pets.forEach(function(pet) {
                                petsList += '<li class="list-group-item">\
                                    <strong>' + pet.name + '</strong> (' + pet.type.name + ')\
                                </li>';
                            });
                        }
                        
                        petsList += '</ul>\
                                    </div>\
                                    <div id="pets-form-container-' + person.id + '" class="mt-3">\
                                    </div>\
                                </div>\
                            </td>\
                        </tr>';
                        
                        personRow.after(petsList);
                        
                        // Initialize PetsView for this person and store it
                        var petsView = new PetsView(
                            self.petsDao,
                            self.typesDao,
                            'pets-form-container-' + person.id,
                            'pets-list-container-' + person.id,
                            person.id
                        );
                        petsViews[person.id] = petsView;
                        petsView.init();
                    },
                    function(error) {
                        console.error('Error fetching pets:', error);
                        showErrorMessage(error);
                    }
                );
            } catch (e) {
                console.error('Exception in view-pets handler:', e);
                showErrorMessage(e);
            }
        });
    };

    var appendToTable = function(person) {
        $(listQuery + ' > tbody:last')
            .append(createPersonRow(person));
        addRowListeners(person);
    };
    
    return PeopleView;
})();
