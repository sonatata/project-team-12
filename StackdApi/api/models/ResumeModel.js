'use strict';
var mongoose = require('mongoose');

var Schema = mongoose.Schema;
var ResumeSchema = new Schema({
	/* recruiter who collect the resume */
	// rId: {
	// 	type: Schema.ObjectId, 
	// 	ref: 'Recruiter',
	// 	required: true
	// },
	url: {
		type: String, 
		default: ""
	}, 
	candidateName: {
		type: String, 
		required: true
	},
	collectionDate: {
		type: Date, 
		default: Date.now
	}, 
	recruiterComments: {
		type: String, 
		required: true 
	},
	/* list of of tags associated to the resume */
	tags: [{
		type: Schema.ObjectId, 
		ref: 'Tag'
	}],  
	rating: {
		type: Number, 
		min: 0, 
		max: 10, 
		required: true
	}
});

module.export = mongoose.model('Resume', ResumeSchema);