var PetsView = (function () {
    var dao;
    var typeDao;
    var currentOwnerId;
    var self;
    var formId;
    var listId;
    var formQuery;
    var listQuery;

    function PetsView(petsDao, typesDao, formContainerId, listContainerId, ownerId) {
        console.log('Initializing PetsView with ownerId:', ownerId);
        dao = petsDao;
        typeDao = typesDao;
        self = this;
        currentOwnerId = ownerId;

        formId = 'pets-form-' + currentOwnerId;
        listId = 'pets-list-' + currentOwnerId;
        formQuery = '#' + formId;
        listQuery = '#' + listId;

        insertPetsForm($('#' + formContainerId));
        insertPetsList($('#' + listContainerId));

        // Initialize form with types
        console.log('Loading pet types...');
        typeDao.listTypes(
            function(types) {
                console.log('Types loaded successfully:', types);
                loadTypeSelect(types);
            },
            function(error) {
                console.error('Error loading types:', error);
                // Don't show error message since types might still be visible
            }
        );

        this.init = function () {
            refreshPetsList();

            $(formQuery).submit(function (event) {
                event.preventDefault();

                var pet = self.getPetInForm();
                pet.owner = { id: currentOwnerId };

                if (self.isEditing()) {
                    dao.modifyPet(currentOwnerId, pet,
                        function (pet) {
                            $('#pet-' + pet.id + '-owner-' + currentOwnerId + ' td.name').text(pet.name);
                            $('#pet-' + pet.id + '-owner-' + currentOwnerId + ' td.type').text(pet.type.name);
                            self.resetForm();
                            showSuccessMessage('Mascota modificada correctamente');
                        },
                        showErrorMessage,
                        self.enableForm
                    );
                } else {
                    self.disableForm();

                    dao.addPet(currentOwnerId, pet,
                        function (pet) {
                            self.resetForm();
                            refreshPetsList();
                            self.enableForm();
                            showSuccessMessage('Mascota añadida correctamente');
                        },
                        function (error) {
                            showErrorMessage(error);
                            self.enableForm();
                        }
                    );
                }

                return false;
            });

            $('#btnClear-' + currentOwnerId).click(this.resetForm);
        };

        this.getPetInForm = function () {
            var form = $(formQuery);
            var typeId = form.find('select[name="type"]').val();
            return {
                'id': form.find('input[name="id"]').val(),
                'name': form.find('input[name="name"]').val(),
                'type': { 'id': typeId }
            };
        };

        this.getPetInRow = function (id) {
            var row = $('#pet-' + id + '-owner-' + currentOwnerId);

            if (row !== undefined) {
                return {
                    'id': id,
                    'name': row.find('td.name').text(),
                    'type': {
                        'id': row.find('td.type').data('type-id'),
                        'name': row.find('td.type').text()
                    }
                };
            } else {
                return undefined;
            }
        };

        this.editPet = function (id) {
            var row = $('#pet-' + id + '-owner-' + currentOwnerId);

            if (row !== undefined) {
                var form = $(formQuery);

                form.find('input[name="id"]').val(id);
                form.find('input[name="name"]').val(row.find('td.name').text());
                form.find('select[name="type"]').val(row.find('td.type').data('type-id'));

                $('#btnSubmit-' + currentOwnerId).val('Modificar');
                $('#form-title-' + currentOwnerId).text('Modificar mascota:');
            }
        };

        this.deletePet = function (id) {
            if (confirm('Está a punto de eliminar una mascota. ¿Está seguro de que desea continuar?')) {
                dao.deletePet(currentOwnerId, id,
                    function () {
                        $('#pet-' + id + '-owner-' + currentOwnerId).remove();
                        showSuccessMessage('Mascota eliminada correctamente');
                    },
                    showErrorMessage
                );
            }
        };

        this.isEditing = function () {
            return $(formQuery + ' input[name="id"]').val() != "";
        };

        this.disableForm = function () {
            $(formQuery + ' input, ' + formQuery + ' select').prop('disabled', true);
        };

        this.enableForm = function () {
            $(formQuery + ' input, ' + formQuery + ' select').prop('disabled', false);
        };

        this.resetForm = function () {
            $(formQuery)[0].reset();
            $(formQuery + ' input[name="id"]').val('');
            $('#btnSubmit-' + currentOwnerId).val('Crear');
            $('#form-title-' + currentOwnerId).text('Añadir nueva mascota:');
        };
    }

    var loadTypeSelect = function(types) {
        var select = $(formQuery + ' select[name="type"]');
        select.empty();
        
        // Add default option
        select.append($('<option>', {
            value: '',
            text: 'Seleccione un tipo',
            disabled: true,
            selected: true
        }));
        
        // Add types from server
        if (types && types.length > 0) {
            $.each(types, function(i, type) {
                select.append($('<option>', {
                    value: type.id,
                    text: type.name
                }));
            });
            console.log('Added', types.length, 'types to select');
        } else {
            console.warn('No types available to load in select');
        }
    };

    var insertPetsList = function (parent) {
        parent.empty().append(
            '<div id="feedback-' + currentOwnerId + '" class="alert" style="display:none; margin-bottom: 15px;"></div>\
            <table id="' + listId + '" class="table">\
                <thead>\
                    <tr class="row">\
                        <th class="col-sm-4">Nombre</th>\
                        <th class="col-sm-5">Tipo</th>\
                        <th class="col-sm-3">&nbsp;</th>\
                    </tr>\
                </thead>\
                <tbody>\
                </tbody>\
            </table>'
        );
    };

    var insertPetsForm = function (parent) {
        parent.empty().append(
            '<h6 id="form-title-' + currentOwnerId + '">Añadir nueva mascota:</h6>\
            <form id="' + formId + '" class="mb-5 mb-10">\
                <input name="id" type="hidden" value=""/>\
                <div class="row">\
                    <div class="col-sm-4">\
                        <input name="name" type="text" value="" placeholder="Nombre" class="form-control" required/>\
                    </div>\
                    <div class="col-sm-5">\
                        <select name="type" class="form-control" required>\
                            <option value="" disabled selected>Cargando tipos...</option>\
                        </select>\
                    </div>\
                    <div class="col-sm-3">\
                        <input id="btnSubmit-' + currentOwnerId + '" type="submit" value="Crear" class="btn btn-primary" />\
                        <input id="btnClear-' + currentOwnerId + '" type="reset" value="Limpiar" class="btn" />\
                    </div>\
                </div>\
            </form>'
        );
    };

    var createPetRow = function (pet) {
        return '<tr id="pet-' + pet.id + '-owner-' + currentOwnerId + '" class="row">\
            <td class="name col-sm-4">' + pet.name + '</td>\
            <td class="type col-sm-5" data-type-id="' + pet.type.id + '">' + pet.type.name + '</td>\
            <td class="col-sm-3">\
                <a class="edit btn btn-primary btn-sm me-1" href="#">Editar</a>\
                <a class="delete btn btn-warning btn-sm me-1" href="#">Eliminar</a>\
            </td>\
        </tr>';
    };

    var showErrorMessage = function (jqxhr, textStatus, error) {
        var feedback = $('#feedback-' + currentOwnerId);
        feedback.removeClass('alert-success').addClass('alert-danger')
            .text((textStatus || 'Error') + (error ? ": " + error : ''))
            .fadeIn()
            .delay(3000)
            .fadeOut();
    };

    var showSuccessMessage = function (message) {
        var feedback = $('#feedback-' + currentOwnerId);
        feedback.removeClass('alert-danger').addClass('alert-success')
            .text(message)
            .fadeIn()
            .delay(3000)
            .fadeOut();
    };

    var refreshPetsList = function () {
        $(listQuery + ' > tbody').empty();
        dao.listPets(currentOwnerId, function (pets) {
            $.each(pets, function (key, pet) {
                appendToTable(pet);
            });
        },
            function () {
                showErrorMessage(null, 'No ha sido posible acceder al listado de mascotas');
            });
    };

    var appendToTable = function (pet) {
        $(listQuery + ' > tbody:last')
            .append(createPetRow(pet));
        addRowListeners(pet);
    };

    var addRowListeners = function (pet) {
        $('#pet-' + pet.id + '-owner-' + currentOwnerId + ' a.edit').click(function (event) {
            event.preventDefault();
            self.editPet(pet.id);
        });

        $('#pet-' + pet.id + '-owner-' + currentOwnerId + ' a.delete').click(function (event) {
            event.preventDefault();
            self.deletePet(pet.id);
        });
    };

    return PetsView;
})();