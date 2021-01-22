import React, { useState } from "react"
import "./style.css"

export default () => {
  const [state, setState] = useState(0)
  const fetchNew = async () => {
    const result = await fetch('/api/random/')
    const json = await result.json()
    setState(json.message)
  }
  
  return (
    <div>
      The fetched state is '{state}'
      <button onClick={fetchNew}>Fetch new</button>
    </div>
  )
}
