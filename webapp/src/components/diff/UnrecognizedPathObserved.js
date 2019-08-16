import React from 'react';
import withStyles from '@material-ui/core/styles/withStyles';
import Helmet from 'react-helmet';
import Editor, {FullSheet, Sheet} from '../navigation/Editor';
import {withEditorContext} from '../../contexts/EditorContext';
import DiffTopBar from './DiffTopBar';
import {PathEditorWithContext} from '../PathPage';
import DiffCard from './DIffCard';
import CommitChangesModal from './CommitChangesModal';
import {Typography} from '@material-ui/core';
import PathInput from '../path-editor/PathInput';
import pathToRegexp from 'path-to-regexp';
import Button from '@material-ui/core/Button';
import LinearProgress from '@material-ui/core/LinearProgress';
import * as niceTry from 'nice-try'

const styles = theme => ({
	pathWrapper: {
		padding: 7,
		fontWeight: 400,
		backgroundColor: '#f6f6f6'
	},
	root: {
		padding: 17
	}
});

const AllPathsInSession = [
	'/people/aidan/profile/favorites',
	'/people/dev/profile/favorites',
	'/people/dev/profile/pets',
	'/people/pgraham/profile/favorites',
	'/people/pgraham/profile/children',
	'/people/carmine/profile/favorites',
];

function buildRegex(components = []) {

	const pathsToRegexTemplate = '/' + components.map(({name, isParameter}) => {
		if (isParameter) {
			const stripped = name.replace('{', '').replace('}', '');
			return `:${stripped}`;
		} else {
			return `${name}`;
		}
	}).join('/');
	return {
		incremental: pathToRegexp(pathsToRegexTemplate, [], {end: false}),
		complete: pathToRegexp(pathsToRegexTemplate)
	};
}

class UnrecognizedPathObserved extends React.Component {

	state = {
		currentRegex: {incremental: new RegExp('/')},
	};

	onPathChanged = (value) => {
		this.setState({currentRegex: buildRegex(value)});
	};

	render() {
		const {classes, path} = this.props;
		const {currentRegex} = this.state;

		const found = path.match(currentRegex.incremental);

		const completeMatch = niceTry(() => path.match(currentRegex.complete)[0].length === path.length)

		let matched = '';
		let remaining = path;

		if (found && found[0].length) {
			const startString = found[0];
			const start = path.substring(0, startString.length);
			matched = start;
			remaining = path.substring(startString.length);
		}


		return (
			<Sheet>
				<div className={classes.root}>
					<Typography variant="h5">Unrecognized Path Observed</Typography>
					<Typography variant="subtitle2" style={{paddingTop: 11, paddingBottom: 11}}>Optic observed a new path. Before Optic can document the requests you need to add the path to your API specification.</Typography>

					<Typography variant="overline" style={{paddingBottom: 0}}>Path:</Typography>
					<div className={classes.pathWrapper}><span
						style={{color: '#277a4e', fontWeight: 800}}>{matched}</span><span>{remaining}</span></div>


					<Typography variant="overline" style={{marginBottom: 0}}>Provide Path Matcher:</Typography>
					<div className={classes.pathWrapper}>
						<PathInput onChange={this.onPathChanged} onSubmit={console.log} initialPathString={''}/>
					</div>


					<div style={{marginTop: 17, paddingTop: 4, textAlign: 'right'}}>
						<Button>Skip</Button>
						<Button color="secondary" disabled={!completeMatch}>Add Path</Button>
					</div>
				</div>
			</Sheet>
		);
	}
}

export default withEditorContext(withStyles(styles)(UnrecognizedPathObserved));
