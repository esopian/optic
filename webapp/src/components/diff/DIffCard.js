import React, {createRef} from 'react';
import withStyles from '@material-ui/core/styles/withStyles';
import {withEditorContext} from '../../contexts/EditorContext';
import Card from '@material-ui/core/Card';
import {CardHeader} from '@material-ui/core';
import Typography from '@material-ui/core/Typography';
import CardActions from '@material-ui/core/CardActions';
import Button from '@material-ui/core/Button';
import {primary, secondary} from '../../theme';
import CardContent from '@material-ui/core/CardContent';
import IconButton from '@material-ui/core/IconButton';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import CloseIcon from '@material-ui/icons/Close';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import keydown, {Keys} from 'react-keydown';
import TextField from '@material-ui/core/TextField';
import Fade from '@material-ui/core/Fade';

const styles = theme => ({
	header: {
		backgroundColor: primary,
		color: 'white',
		padding: 2,
		paddingLeft: 12
	},
	textField: {
		// marginLeft: theme.spacing(1),
		// marginRight: theme.spacing(1),
		fontSize: 12
	},
});

const ExampleInterpretations = [
	{
		name: 'Create a new Concept',
		description: 'Create a new concept with fields [email, password, firstName, lastName]',
		args: [{name: 'Concept Name', type: 'string'}]
	},
	{
		name: 'Use Concept "Legacy User"',
		description: 'Reference "Legacy User" and select fields [email, password, firstName, lastName]'
	},
	{
		name: 'Add a shape inline',
		description: 'Create an inline shape with fields [email, password, firstName, lastName]'
	},
];


class DiffCard extends React.Component {

	state = {
		showAllInterpretations: false,
		selectedInteraction: 0
	};

	setInteraction = (i) => {
		this.setState({selectedInteraction: i, showAllInterpretations: false});
	};

	switchInterpretation = () => {
		this.setState({showAllInterpretations: !this.state.showAllInterpretations});
	};

	render() {
		const {classes} = this.props;
		const {showAllInterpretations, selectedInteraction} = this.state;
		const interaction = ExampleInterpretations[selectedInteraction];
		const title = 'Request Body Observed'

		return (
			<Card className={classes.root} style={{marginTop: 120}} elevation={3}>
				<CardHeader title={
					<div style={{display: 'flex'}}>
						<Typography variant="subtitle1" style={{marginTop: 2}}>{title}</Typography>
						<div style={{flex: 1}}/>

						<Typography variant="subtitle2" style={{marginTop: 6, fontSize: 12}}>(3)</Typography>
						<IconButton color={'secondary'} size="small" style={{color: 'white'}}
									onClick={this.switchInterpretation}>
							{showAllInterpretations ? <CloseIcon style={{height: 15}}/> :
								<MoreVertIcon style={{height: 15}}/>}
						</IconButton>

					</div>
				} className={classes.header}/>
				<CardContent style={{padding: 0}}>
					{(showAllInterpretations) ? (
						<Fade in={true}>
						<List>
							{ExampleInterpretations.map((interpretation, index) => {
								return <ListItem
									button
									onClick={() => this.setInteraction(index)}
								><ListItemText secondary={interpretation.name}
											   secondaryTypographyProps={{color: '#202020'}}/></ListItem>;
							})}
						</List>
						</Fade>
					) : (
						<div>

							<Fade in={true}>
							<div style={{padding: 11}}>
								<Typography variant="overline" component="div" style={{fontSize: 16, textAlign: 'left'}}>{interaction.name}</Typography>
								<Typography variant="subtitle2" style={{color: '#5c5c5c'}}>{interaction.description}</Typography>

								{(interaction.args || []).map(({name, type}) => {
									if (type === 'string') {
										return <TextField
											id="outlined-name"
											label={name}
											fullWidth={true}
											className={classes.textField}
											// value={values.name}
											// onChange={handleChange('name')}
											margin="normal"
										/>
									}
								})}

							</div>

							</Fade>
							<CardActions>
								<Button size="small" color="primary">
									Approve
								</Button>
								<Button size="small" color="secondary">
									Ignore
								</Button>
							</CardActions>
						</div>
					)}
				</CardContent>

			</Card>);
	}
}

export default withEditorContext(withStyles(styles)(DiffCard));
