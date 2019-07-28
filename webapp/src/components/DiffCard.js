import React, {useState} from 'react';
import withStyles from '@material-ui/core/styles/withStyles';
import Card from '@material-ui/core/Card';
import {CardHeader} from '@material-ui/core';
import Typography from '@material-ui/core/Typography';
import CardActions from '@material-ui/core/CardActions';
import Button from '@material-ui/core/Button';
import CardContent from '@material-ui/core/CardContent';
import Zoom from '@material-ui/core/Zoom';

const styles = theme => ({
	card: {
		margin: 22,
	},
	greenLine: {
		backgroundColor: 'rgba(73,228,146,0.4)',
		width: '100%',
		paddingLeft: 9,
		marginTop: 4
	},
	redLine: {
		backgroundColor: 'rgba(139,44,58,0.4)',
		width: '100%',
		paddingLeft: 9,
		marginTop: 4
	}
});

export const MockedDiffCards = withStyles(styles)(({path, classes}) => {
	const items = {
		'GBsKDC_path_1': [
			{
				offsetY: 250,
				title: 'Removed Query Parameter',
				changes: [
					<li className={classes.redLine}><Typography variant="paragraph">Removed 'filter'</Typography></li>
				]
			},
			{
				offsetY: 400,
				title: 'Updated Response Body',
				changes: [
					<li className={classes.greenLine}><Typography variant="paragraph">Added field
						'location'</Typography>
					</li>,
					<li className={classes.greenLine}><Typography variant="paragraph">Updated type of field
						'radius'</Typography></li>,
					<li className={classes.redLine}><Typography variant="paragraph">Removed field 'isAdmin'</Typography>
					</li>
				]
			}
		]
	};

	const matchedCards = items[path] || [];

	return <div>
		{matchedCards.map(i => <DiffCard {...i}/>)}
	</div>;
});

export const DiffCard = withStyles(styles)(({classes, title, changes, offsetY}) => {

	const [isVisible, setIsVisible] = useState(true)

	return <Zoom in={isVisible} appear={false}><Card className={classes.card} elevation={3} style={{marginTop: offsetY}}>
		<CardHeader style={{padding: 6}}
					avatar={<img src="/optic-logo-new.svg" width={40}/>}
					title={<Typography variant="subtitle1" style={{marginTop: -2}}>{title}</Typography>}
		/>
		<CardContent style={{marginTop: -20}}>
			{changes}
		</CardContent>
		<CardActions style={{display: 'flex'}}>
			<div style={{flex: 1}}/>
			<Button size="small" color="secondary" onClick={() => setIsVisible(false)}>Mark as Error</Button>
			<Button size="small" color="primary" variant="contained" onClick={() => setIsVisible(false)}>Accept</Button>
		</CardActions>
	</Card></Zoom>;
});
