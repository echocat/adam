/*****************************************************************************************
 * *** BEGIN LICENSE BLOCK *****
 *
 * echocat Adam, Copyright (c) 2014 echocat
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *
 * *** END LICENSE BLOCK *****
 ****************************************************************************************/

if (typeof org == 'undefined')  var org = {};
if (typeof org.echocat == 'undefined') org.echocat = {};
if (typeof org.echocat.adam == 'undefined') org.echocat.adam = {};

org.echocat.adam.ElementsField = function(parameter) {
    var _this = this;
    this.parameter = parameter;
    this.paramDiv = this._createDiv();
    this.title = this._createTitle();
    this.viewSelect = this._createViewSelect();
    this.nodes = this._createNodes();
    this.input = this._createInput();

    this.paramDiv.append(this.title).append(this.input).append(this.viewSelect).append(this.nodes);

    this._loadModel(function(model) {
        _this._createOptionsFrom(model);
        _this._loadViews(function(views) {
            _this._createViews(views);
            _this.loaded = true;
            _this.setValue(_this.input.val());
        });
    });

    return this;
};

org.echocat.adam.ElementsField.prototype = {
    view: 'custom',
    views: [],
    loaded: false,
    _loadViews: function(onSuccess) {
        var contextPath = $('meta[id=confluence-context-path]').attr('content') || '';
        var uri = contextPath + "/rest/adam/latest/view";
        $.ajax({
            dataType: "json",
            url: uri,
            success: onSuccess
        });
    },
    _loadModel: function(onSuccess) {
        var contextPath = $('meta[id=confluence-context-path]').attr('content') || '';
        var uri = contextPath + "/rest/adam/latest/model/profile";
        $.ajax({
            dataType: "json",
            url: uri,
            success: onSuccess
        });
    },
    getValue: function() {
        this.inUpdate = true;
        try {
            var result;
            if (this.loaded) {
                if (this.view != 'custom') {
                    result = '$$view:' + (this.view || 'default') + '$$';
                } else {
                    result = '';
                    this.nodes.find('input:checked').each(function() {
                        var qthis = $(this);
                        if (result != '') {
                            result += ',';
                        }
                        result += qthis.attr('profile-element');
                    });
                }
            } else {
                result = this.input.val();
            }
            return result;
        } finally {
            this.inUpdate = false;
        }
    },
    setValue: function(value) {
        this.inUpdate = true;
        try {
            this.input.val(value);
            if (this.loaded) {
                var view = this._tryExtractViewFrom(value);
                if (view && view != 'custom') {
                    this._setViewTo(view);
                } else {
                    this.view = 'custom';
                    this.viewSelect.val(this.view);
                    this.nodes.find('input[type=checkbox]').attr('checked', false);
                    var ids = value.split(',');
                    for (var i = 0; i < ids.length; i++) {
                        var id = ids[i];
                        this.nodes.find('input[type=checkbox][profile-element=' + id + ']').attr('checked', true);
                    }
                }
            }
        } finally {
            this.inUpdate = false;
        }
    },
    _tryExtractViewFrom: function(value) {
        var result;
        if (value && value.match instanceof Function) {
            var match = value.match(/^\$\$view:([a-zA-Z0-9_\-]+)\$\$$/);
            if (match) {
                result = match[1];
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    },
    onCheckBoxesChange: function() {
        this.inUpdate = true;
        try {
            this.input.val(this.getValue());
        } finally {
            this.inUpdate = false;
        }
    },
    onInputChange: function() {
        if (!this.inUpdate) {
            this.setValue(this.input.val());
        }
    },
    onViewChange: function() {
        if (!this.inUpdate) {
            this._setViewTo(this.viewSelect.val());
        }
    },
    _setViewTo: function(view, force) {
        if (force || view != this.view) {
            this.inUpdate = true;
            try {
                this.view = view;
                if (view != 'custom') {
                    this.nodes.find('input').attr('checked', false).attr('disabled', true);
                    this.nodes.find('span.profile-element-label').attr('disabled', true);
                    this.nodes.find('input[for-' + view + '-view]').attr('checked', true);
                    this.input.val('$$view:' + view + '$$');
                } else {
                    this.nodes.find('input').attr('disabled', false);
                    this.nodes.find('span.profile-element-label').attr('disabled', false);
                    this.input.val(this.getValue());
                }
                this.viewSelect.val(view);
            } finally {
                this.inUpdate = false;
            }
        }
    },
    _createDiv: function() {
        return $('<div class="macro-param-div" />');
    },
    _createInput: function() {
        var result = $('<input type="hidden" />');
        result.attr('name', this.parameter.id);
        var _this = this;
        result.change(function() {
            _this.onInputChange();
        });
        return result;
    },
    _createTitle: function() {
        var result = $('<label for="macro-param-' + this.parameter.name + '" />');
        result.text(this.parameter.displayName);
        return result;
    },
    _buildViewMapBy: function(views) {
        var result = {};
        for (var i = 0; views instanceof Array && i < views.length; i++) {
            var view = views[i];
            var elementIds = {};
            for (var gi = 0;  view.groups instanceof Array && gi < view.groups.length; gi++) {
                var group = view.groups[gi];
                for (var ei = 0;  group.elements instanceof Array && ei < group.elements.length; ei++) {
                    var element = group.elements[ei];
                    elementIds[element.id] = element.label;
                }
            }
            result[view.id] = elementIds;
        }
        return result;
    },
    _reassignViewsAttributes: function(views) {
        this.nodes.find('input').each(function() {
            var attributesToRemove = [];
            for (var attribute in this.attributes) {
                //noinspection JSUnfilteredForInLoop
                var match = attribute.match(/^for-([a-zA-Z0-9_\-]+)-view$/);
                if (match) {
                    //noinspection JSUnfilteredForInLoop
                    attributesToRemove.push(attribute);
                }
            }
            for (var i = 0; i < attributesToRemove.length; i++) {
                this.removeAttribute(attributesToRemove[i]);
            }
        });
        for (var view in views) {
            //noinspection JSUnfilteredForInLoop
            var elementIds = views[view];
            for (var elementId in elementIds) {
                this.nodes.find('input[profile-element=' + elementId + ']').attr('for-' + view + '-view', '');
            }
        }
    },
    _createViewSelect: function() {
        var result = $('<select class="profile-view-select"><option>...</option></select>');
        var _this = this;
        result.change(function() {
            _this.onViewChange();
        });
        return result;
    },
    _createViews: function(views) {
        this.views = this._buildViewMapBy(views);
        this._reassignViewsAttributes(this.views);
        this.viewSelect.empty();
        for (var i = 0; views instanceof Array && i < views.length; i++) {
            var view = views[i];
            this.viewSelect.append(this._createViewOptionFrom(view));
        }
        var customOption = $('<option value="custom" />');
        customOption.text(AJS.I18n.getText("org.echocat.adam.custom"));
        this.viewSelect.append(customOption);
        return this.viewSelect;
    },
    _createViewOptionFrom: function(view) {
        var result = $('<option />');
        result.attr('value', view.id);
        result.text(view.label);
        return result;
    },
    _createNodes: function() {
        return $('<ul class="adam-profile-marco elementsSelection"><li>...</li></ul>');
    },
    _createOptionsFrom: function(model) {
        this.nodes.empty();
        if (model) {
            for (var i = 0; model.groups instanceof Array && i < model.groups.length; i++) {
                var group = model.groups[i];
                this.nodes.append(this._createGroupFrom(group));
            }
        }
        return this.nodes;
    },
    _createGroupFrom: function(group) {
        var result = $('<li class="profile-group" />');
        var label = $('<div class="profile-group-label" />');
        label.attr('id', 'profile-group-' + group.id);
        label.attr('profile-group', group.id);
        label.text(group.label);

        var elements = $('<ul />');
        for (var i = 0; group.elements instanceof Array && i < group.elements.length; i++) {
            var element = group.elements[i];
            elements.append(this._createElementFor(element));
        }

        result.append(label).append(elements);
        return result;
    },
    _createElementFor: function(element) {
        var result = $('<li />');
        var checkBox = this._createCheckBoxFor(element);
        result.append(checkBox);
        result.append(this._createLabelFor(element, checkBox));
        return result;
    },
    _createCheckBoxFor: function(element) {
        var result = $('<input type="checkbox" />');
        result.attr('id', 'profile-element-' + element.id);
        result.attr('profile-element', element.id);
        result.attr('value', element.id);
        var _this = this;
        result.change(function() {
            _this.onCheckBoxesChange();
        });
        return result;
    },
    _createLabelFor: function(element, checkBox) {
        var _this = this;
        var result = $('<span class="profile-element-label" />');
        result.text(element.label);
        result.click(function() {
            if (!$(this).attr('disabled')) {
                checkBox.attr("checked", !checkBox.attr("checked"));
                _this.onCheckBoxesChange();
            }
        });
        return result;
    }
};

AJS.bind("init.rte", function() {
    var override = AJS.MacroBrowser.getMacroJsOverride("user-profile");
    if (!override) {
        override = {};
        AJS.MacroBrowser.setMacroJsOverride("user-profile", override)
    }
    if (!override.fields) {
        override.fields = {};
    }
    if (!override.fields.string) {
        override.fields.string = {};
    }
    override.fields.string.elements = function(parameter) {
        return new org.echocat.adam.ElementsField(parameter);
    };
});

