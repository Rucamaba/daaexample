var PetsView = (function () {
    var dao;
    var typeDao;
    var currentOwnerId;
    var self;
    var formId;
    var listId;
    var formQuery;
    var listQuery;
    var currentTypes = []; // Store current types list

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

        const userRole = localStorage.getItem('user-role');
        const isAdmin = userRole === 'ADMIN';

        if (isAdmin) {
            insertPetsForm($('#' + formContainerId));
            insertTypeModal();
        }
        insertPetsList($('#' + listContainerId));

        // Initialize form with types
        console.log('Loading pet types...');
        typeDao.listTypes(
            function(types) {
                console.log('Types loaded successfully:', types);
                currentTypes = types; // Store types list
                loadTypeSelect(types);
                loadTypesList(types); // Load types in the modal's list
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

            // Add handler for type select change
            $(formQuery + ' select[name="type"]').change(function() {
                if ($(this).val() === 'other') {
                    $('#typeModal-' + currentOwnerId).modal('show');
                    $('#customTypeName-' + currentOwnerId).val('').focus();
                }
            });

            // Add handler for type search input
            $('#customTypeName-' + currentOwnerId).on('input', function() {
                var searchText = $(this).val().toLowerCase();
                filterTypesList(searchText);
            });

            // Add handler for type list item click
            $('#typesList-' + currentOwnerId).on('click', '.type-item', function() {
                var typeId = $(this).data('type-id');
                var select = $(formQuery + ' select[name="type"]');
                select.val(typeId);
                $('#typeModal-' + currentOwnerId).modal('hide');
                showSuccessMessage('Tipo seleccionado');
            });

            // Add handler for custom type form submit
            $('#customTypeForm-' + currentOwnerId).submit(function(event) {
                event.preventDefault();
                var customTypeName = $('#customTypeName-' + currentOwnerId).val();
                
                if (customTypeName) {
                    // Check if type already exists (case-insensitive)
                    var existingType = currentTypes.find(function(type) {
                        return type.name.toLowerCase() === customTypeName.toLowerCase();
                    });

                    if (existingType) {
                        // If type exists, just select it
                        var select = $(formQuery + ' select[name="type"]');
                        select.val(existingType.id);
                        $('#typeModal-' + currentOwnerId).modal('hide');
                        $('#customTypeName-' + currentOwnerId).val('');
                        showSuccessMessage('Tipo seleccionado');
                    } else {
                        // If type doesn't exist, create new one
                        typeDao.addType({ name: customTypeName },
                            function(newType) {
                                // Add the new type to both the select and our current types list
                                currentTypes.push(newType);
                                var select = $(formQuery + ' select[name="type"]');
                                select.find('option[value="other"]').before(
                                    $('<option>', {
                                        value: newType.id,
                                        text: newType.name
                                    })
                                );
                                select.val(newType.id);
                                loadTypesList(currentTypes); // Refresh types list
                                $('#typeModal-' + currentOwnerId).modal('hide');
                                $('#customTypeName-' + currentOwnerId).val('');
                            },
                            function(error) {
                                showErrorMessage(error);
                            }
                        );
                    }
                }
                return false;
            });
        };

        this.cleanup = function() {
            // Remove the modal
            $('#typeModal-' + currentOwnerId).remove();
            // Clear the form and list containers
            $('#' + formContainerId).empty();
            $('#' + listContainerId).empty();
        };

        this.getPetInForm = function () {
            var form = $(formQuery);
            var typeId = form.find('input[name="type"]').val();
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

        // Add "Other" option
        select.append($('<option>', {
            value: 'other',
            text: 'Otro'
        }));
    };

    var loadTypesList = function(types) {
        var list = $('#typesList-' + currentOwnerId);
        list.empty();
        
        if (types && types.length > 0) {
            $.each(types, function(i, type) {
                list.append(
                    $('<div>', {
                        class: 'type-item',
                        'data-type-id': type.id,
                        text: type.name
                    })
                );
            });
        }
    };

    var filterTypesList = function(searchText) {
        $('.type-item').each(function() {
            var typeName = $(this).text().toLowerCase();
            if (typeName.includes(searchText)) {
                $(this).show();
            } else {
                $(this).hide();
            }
        });
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

    var insertTypeModal = function() {
        $('body').append('\
            <div class="modal fade" id="typeModal-' + currentOwnerId + '" tabindex="-1" aria-hidden="true">\
                <div class="modal-dialog">\
                    <div class="modal-content">\
                        <div class="modal-header">\
                            <h5 class="modal-title">Especificar tipo de mascota</h5>\
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>\
                        </div>\
                        <div class="modal-body">\
                            <form id="customTypeForm-' + currentOwnerId + '">\
                                <div class="mb-3">\
                                    <label for="customTypeName-' + currentOwnerId + '" class="form-label">Buscar o añadir nuevo tipo</label>\
                                    <div class="input-group mb-2">\
                                        <span class="input-group-text"><i class="fas fa-search"></i></span>\
                                        <input type="text" class="form-control form-control-lg" \
                                            id="customTypeName-' + currentOwnerId + '" \
                                            placeholder="Escriba para buscar o añadir tipo" \
                                            required autocomplete="off">\
                                    </div>\
                                    <div id="typesList-' + currentOwnerId + '" class="types-list">\
                                    </div>\
                                </div>\
                                <div class="mt-3 d-flex justify-content-end">\
                                    <button type="button" class="btn btn-secondary me-2" data-bs-dismiss="modal">Cancelar</button>\
                                    <button type="submit" class="btn btn-primary">Guardar nuevo tipo</button>\
                                </div>\
                            </form>\
                        </div>\
                    </div>\
                </div>\
            </div>\
            <style>\
                .types-list {\
                    max-height: 300px;\
                    overflow-y: auto;\
                    margin-top: 10px;\
                    border: 1px solid #dee2e6;\
                    border-radius: 4px;\
                    background-color: white;\
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);\
                }\
                .type-item {\
                    padding: 12px 16px;\
                    cursor: pointer;\
                    transition: background-color 0.2s;\
                    background-color: white;\
                    font-size: 16px;\
                }\
                .type-item:hover {\
                    background-color: #e9ecef;\
                }\
                .type-item:not(:last-child) {\
                    border-bottom: 1px solid #dee2e6;\
                }\
                #typeModal-' + currentOwnerId + ' .modal-body {\
                    padding: 20px;\
                }\
                #typeModal-' + currentOwnerId + ' .form-control:focus {\
                    border-color: #80bdff;\
                    box-shadow: 0 0 0 0.2rem rgba(0,123,255,.25);\
                }\
                #typeModal-' + currentOwnerId + ' .input-group-text {\
                    background-color: white;\
                    border-right: none;\
                }\
                #customTypeName-' + currentOwnerId + ' {\
                    border-left: none;\
                }\
            </style>'
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
                        <div class="position-relative">\
                            <input type="text" class="form-control" id="typeSearch-' + currentOwnerId + '" \
                                placeholder="Buscar o escribir tipo..." required autocomplete="off">\
                            <input type="hidden" name="type" id="typeId-' + currentOwnerId + '" required>\
                            <div id="typesList-' + currentOwnerId + '" class="types-dropdown" style="display: none;">\
                            </div>\
                        </div>\
                    </div>\
                    <div class="col-sm-3">\
                        <input id="btnSubmit-' + currentOwnerId + '" type="submit" value="Crear" class="btn btn-primary" />\
                        <input id="btnClear-' + currentOwnerId + '" type="reset" value="Limpiar" class="btn" />\
                    </div>\
                </div>\
            </form>\
            <style>\
                .types-dropdown {\
                    position: absolute;\
                    top: 100%;\
                    left: 0;\
                    right: 0;\
                    max-height: 200px;\
                    overflow-y: auto;\
                    background: white;\
                    border: 1px solid #dee2e6;\
                    border-radius: 4px;\
                    margin-top: 2px;\
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);\
                    z-index: 1000;\
                }\
                .type-item {\
                    padding: 8px 12px;\
                    cursor: pointer;\
                    transition: background-color 0.2s;\
                }\
                .type-item:hover {\
                    background-color: #e9ecef;\
                }\
                .type-item:not(:last-child) {\
                    border-bottom: 1px solid #dee2e6;\
                }\
                .type-item.create-new {\
                    border-top: 2px solid #dee2e6;\
                    font-style: italic;\
                    color: #0d6efd;\
                }\
            </style>'
        );

        // Add handlers for type search
        var $searchInput = $('#typeSearch-' + currentOwnerId);
        var $typesList = $('#typesList-' + currentOwnerId);
        var $typeIdInput = $('#typeId-' + currentOwnerId);

        // Function to show the types list
        function showTypesList() {
            var searchText = $searchInput.val().toLowerCase();
            updateTypesList(searchText);
            $typesList.show();
        }

        // Function to hide the types list
        function hideTypesList() {
            setTimeout(() => $typesList.hide(), 200);
        }

        // Function to update the types list based on search
        function updateTypesList(searchText) {
            $typesList.empty();
            
            // Filter and add matching types
            var hasMatches = false;
            currentTypes.forEach(function(type) {
                if (!searchText || type.name.toLowerCase().includes(searchText)) {
                    hasMatches = true;
                    $typesList.append(
                        $('<div>', {
                            class: 'type-item',
                            'data-type-id': type.id,
                            'data-type-name': type.name,
                            text: type.name
                        })
                    );
                }
            });

            // Add "Crear nuevo tipo" option if we have text and no exact match
            if (searchText && !currentTypes.find(type =>
                type.name.toLowerCase() === searchText.toLowerCase()
            )) {
                $typesList.append(
                    $('<div>', {
                        class: 'type-item create-new',
                        'data-action': 'create',
                        text: 'Crear nuevo tipo: "' + searchText + '"'
                    })
                );
            }
        }

        // Handle input changes
        $searchInput.on('input focus', function() {
            showTypesList();
        });

        $searchInput.on('blur', hideTypesList);

        // Handle type selection
        $typesList.on('mousedown', '.type-item', function(e) {
            e.preventDefault(); // Prevent input blur from hiding list
            var $item = $(this);
            
            if ($item.data('action') === 'create') {
                // Create new type
                var newTypeName = $searchInput.val();
                typeDao.addType({ name: newTypeName },
                    function(newType) {
                        currentTypes.push(newType);
                        $searchInput.val(newType.name);
                        $typeIdInput.val(newType.id);
                        $typesList.hide();
                        showSuccessMessage('Nuevo tipo creado: ' + newType.name);
                    },
                    function(error) {
                        showErrorMessage(error);
                    }
                );
            } else {
                // Select existing type
                var typeName = $item.data('type-name');
                var typeId = $item.data('type-id');
                $searchInput.val(typeName);
                $typeIdInput.val(typeId);
                $typesList.hide();
            }
        });

        // Update input value when editing
        this.editPet = function(id) {
            var pet = self.getPetInRow(id);
            if (pet) {
                $searchInput.val(pet.type.name);
                $typeIdInput.val(pet.type.id);
            }
        };

        // Clear values on form reset
        $('#btnClear-' + currentOwnerId).click(function() {
            $searchInput.val('');
            $typeIdInput.val('');
        });
    };

    var createPetRow = function (pet) {
        const userRole = localStorage.getItem('user-role');
        const isAdmin = userRole === 'ADMIN';
        
        var actionButtons = '';
        if (isAdmin) {
            actionButtons =
                '<a class="edit btn btn-primary btn-sm me-1" href="#">Editar</a>' +
                '<a class="delete btn btn-warning btn-sm me-1" href="#">Eliminar</a>';
        }
        
        return '<tr id="pet-' + pet.id + '-owner-' + currentOwnerId + '" class="row">\
            <td class="name col-sm-4">' + pet.name + '</td>\
            <td class="type col-sm-5" data-type-id="' + pet.type.id + '">' + pet.type.name + '</td>\
            <td class="col-sm-3">' + actionButtons + '</td>\
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