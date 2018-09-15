import React, { Component } from 'react';

class APIResponse extends Component {
  render() {
    if(this.props.response == null)
      return (<div/>);
    else
      return ( <pre>{this.props.response}</pre> );
  }
}

class QueryAPI extends Component {

  constructor(props) {
    super(props);
    this.state = { response: null };
  }

  handleClick = () => {
    fetch('https://jsonplaceholder.typicode.com/todos/1')
      .then(response => {
        if (response.status === 200)
          return response.json();
        else
          return { status: response.status, message: response.statusText }
      })
      .then(json => this.setState((state, props) => ({
        response: JSON.stringify(json, null, 2)
      })))
      .catch(err => {
        this.setState((state, props) => ({ response: err.toString() }))
      })
  }

  render() {
    return (
      <div className="QueryAPI">
        <button onClick={this.handleClick}>Send API request</button>
        <APIResponse response={this.state.response}/>
      </div>
    );
  }
}

export default QueryAPI;
