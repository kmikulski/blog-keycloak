import React, { Component } from 'react';
import QueryAPI from './QueryAPI';

class Welcome extends Component {

  render() {
    return (
      <div className="Welcome">
        <p>This is your public-facing component.</p>
        <QueryAPI/>
      </div>
    );
  }
}

export default Welcome;
