import React from 'react'
import withStyles from '@material-ui/core/styles/withStyles';
import Helmet from 'react-helmet';
import Editor, {Sheet} from '../navigation/Editor';
import {withEditorContext} from '../../contexts/EditorContext';
import DiffTopBar from './DiffTopBar'
import {PathEditorWithContext} from '../PathPage';

const styles = theme => ({
	root: {
		paddingTop: theme.spacing(2)
	},
});

class DiffPage extends React.Component {
	render() {

		const {classes} = this.props

		return <Editor baseUrl={this.props.baseUrl}
		topBar={<DiffTopBar />}>
			<Helmet><title>{"Review Proposed Changes"}</title></Helmet>
			<div className={classes.root}>
				<div style={{paddingTop: 2}}>
					<PathEditorWithContext pathId={'abcdef'} />
				</div>
			</div>
		</Editor>
	}
}

export default withEditorContext(withStyles(styles)(DiffPage))
