import React from 'react'
import withStyles from '@material-ui/core/styles/withStyles';
import Helmet from 'react-helmet';
import Editor, {Sheet} from '../navigation/Editor';
import {withEditorContext} from '../../contexts/EditorContext';
import DiffTopBar from './DiffTopBar'
import {PathEditorWithContext} from '../PathPage';
import DiffCard from './DIffCard'
import {Modal} from '@material-ui/core';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import TextField from '@material-ui/core/TextField';
import DialogActions from '@material-ui/core/DialogActions';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import ForumIcon from '@material-ui/icons/Forum'
import {secondary} from '../../theme';

const styles = theme => ({
	root: {
		paddingTop: theme.spacing(2)
	},
	iconWrapper: {
		textAlign: 'center',
		padding: 22,
		paddingBottom: 6
	}
});

class CommitChangesDialog extends React.Component {
	render() {
		const {classes, open} = this.props

		return <Dialog open={open}>
			<div className={classes.iconWrapper}><ForumIcon style={{width: 45, height: 45, color: secondary}}/></div>
			<DialogTitle>Commit your changes to the specification</DialogTitle>
			<DialogContent style={{marginTop: -22}}>
				<Typography variant="subtitle2">What did you change about this endpoint? Why?</Typography>
				<TextField
					label="Message (optional)"
					defaultValue=""
					margin="normal"
					multiline={true}
					fullWidth={true}
				/>
			</DialogContent>

			<DialogActions>
				<Button color="default">
					Revert
				</Button>
				<Button color="primary" autoFocus>
					Commit Changes
				</Button>
			</DialogActions>
		</Dialog>
	}
}

export default withEditorContext(withStyles(styles)(CommitChangesDialog))
