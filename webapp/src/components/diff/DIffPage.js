import React from 'react'
import withStyles from '@material-ui/core/styles/withStyles';
import Helmet from 'react-helmet';
import Editor, {Sheet} from '../navigation/Editor';
import {withEditorContext} from '../../contexts/EditorContext';
import DiffTopBar from './DiffTopBar'
import {PathEditorWithContext} from '../PathPage';
import DiffCard from './DIffCard'
import CommitChangesModal from './CommitChangesModal';
import UnrecognizedPathObserved from './UnrecognizedPathObserved';


const styles = theme => ({
	root: {
		paddingTop: theme.spacing(2)
	},
	cardViewRoot: {
		paddingLeft: 22,
		paddingTop: theme.spacing(2),
		paddingRight: 22
	}
});

class DiffPage extends React.Component {
	render() {

		const {classes} = this.props
		const diffCardView = <div className={classes.cardViewRoot}>
			<DiffCard />
		</div>

		return <Editor
			baseUrl={this.props.baseUrl}
			topBar={<DiffTopBar />}
			// rightMargin={diffCardView}
		>
			<Helmet><title>{"Review Proposed Changes"}</title></Helmet>
			<div className={classes.root}>
				<div style={{paddingTop: 2}}>
					{/*<PathEditorWithContext pathId={'wOdXEt_path_0'} />*/}
					<UnrecognizedPathObserved path={'/people/aidan/profile/favorites'} />
				</div>
			</div>
			<CommitChangesModal open={false} />
		</Editor>
	}
}

export default withEditorContext(withStyles(styles)(DiffPage))
