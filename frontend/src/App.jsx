import { useEffect, useState } from 'react'
import './App.css'

const initialField = { name: 'name', type: 'String', required: true }
const storageKey = 'ai-builder-user'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const apiOriginLabel = apiBaseUrl || 'http://127.0.0.1:8080'

const emptyTester = {
  url: '',
  method: 'GET',
  headers: '{\n  "Content-Type": "application/json"\n}',
  body: '',
}

function App() {
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = window.localStorage.getItem(storageKey)
    if (!saved) return null

    try {
      const parsed = JSON.parse(saved)
      return parsed?.token ? parsed : null
    } catch {
      window.localStorage.removeItem(storageKey)
      return null
    }
  })
  const [summary, setSummary] = useState(null)
  const [projects, setProjects] = useState([])
  const [apiHistory, setApiHistory] = useState([])
  const [activeProjectId, setActiveProjectId] = useState('')
  const [activeApiId, setActiveApiId] = useState(null)
  const [selectedFileIndex, setSelectedFileIndex] = useState(0)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [authMode, setAuthMode] = useState('login')
  const [authForm, setAuthForm] = useState({
    name: '',
    email: '',
    password: '',
  })
  const [projectForm, setProjectForm] = useState({
    name: 'Product Service',
    description: 'AI-generated CRUD service for internal catalog workflows',
  })
  const [generatorForm, setGeneratorForm] = useState({
    entityName: 'Product',
    description: 'Create a product management API with validations and a clean DTO-based architecture.',
    rawPrompt: 'Create product API with create, list, update and delete operations.',
    includeJwt: false,
    multiEntity: false,
    fields: [
      { name: 'id', type: 'Long', required: true },
      { name: 'name', type: 'String', required: true },
      { name: 'price', type: 'BigDecimal', required: true },
      { name: 'description', type: 'String', required: false },
    ],
  })
  const [testerForm, setTesterForm] = useState(emptyTester)
  const [testerResponse, setTesterResponse] = useState(null)
  const selectedProject = projects.find((project) => Number(activeProjectId) === project.id) ?? null
  const activeApi = apiHistory.find((item) => item.id === activeApiId) ?? apiHistory[0]
  const activeFile = activeApi?.files?.[selectedFileIndex]

  useEffect(() => {
    if (!currentUser?.token) {
      window.localStorage.removeItem(storageKey)
      return
    }

    window.localStorage.setItem(storageKey, JSON.stringify(currentUser))
    hydrateSession()
  }, [currentUser?.token])

  useEffect(() => {
    const preset = deriveTesterPreset(activeApi)
    if (!preset) {
      return
    }

    setTesterForm((current) => ({
      ...current,
      url: preset.url,
      method: 'GET',
      body: '',
    }))
  }, [activeApi?.id])

  async function hydrateSession() {
    setLoading(true)
    try {
      const profile = await fetchJson('/api/auth/me')
      setCurrentUser((current) => ({ ...current, ...profile }))
      await loadWorkspace()
    } catch (error) {
      if (
        error.message.includes('authorized')
        || error.message.includes('expired')
        || error.message.includes('Forbidden')
      ) {
        handleLogout('Your session expired. Please log in again.')
      } else {
        setMessage(error.message)
      }
    } finally {
      setLoading(false)
    }
  }

  async function loadWorkspace(preferredProjectId = null) {
    setLoading(true)
    try {
      const [summaryData, projectsData] = await Promise.all([
        fetchJson('/api/dashboard/summary'),
        fetchJson('/api/projects'),
      ])

      setSummary(summaryData)
      setProjects(projectsData)

      const resolvedProjectId = preferredProjectId ?? activeProjectId ?? projectsData[0]?.id ?? ''
      setActiveProjectId(resolvedProjectId)

      if (resolvedProjectId) {
        await loadProjectApis(resolvedProjectId)
      } else {
        setApiHistory([])
        setActiveApiId(null)
        setSelectedFileIndex(0)
      }
      setMessage('')
    } catch (error) {
      if (
        error.message.includes('authorized')
        || error.message.includes('expired')
        || error.message.includes('Forbidden')
      ) {
        handleLogout('Your session expired. Please log in again.')
      } else {
        setMessage(error.message)
      }
    } finally {
      setLoading(false)
    }
  }

  async function loadProjectApis(projectId) {
    if (!projectId) {
      setApiHistory([])
      setActiveApiId(null)
      setSelectedFileIndex(0)
      return
    }

    const history = await fetchJson(`/api/generation/project/${projectId}`)
    setApiHistory(history)
    setActiveApiId(history[0]?.id ?? null)
    setSelectedFileIndex(0)
  }

  async function handleAuthSubmit(event) {
    event.preventDefault()
    setSubmitting(true)

    try {
      const trimmedName = authForm.name.trim()
      const trimmedEmail = authForm.email.trim()
      const trimmedPassword = authForm.password.trim()

      if (authMode === 'register' && !trimmedName) {
        throw new Error('Enter your full name to create an account.')
      }
      if (!trimmedEmail) {
        throw new Error('Enter your email address.')
      }
      if (!trimmedPassword) {
        throw new Error('Enter your password.')
      }
      if (authMode === 'register' && trimmedPassword.length < 6) {
        throw new Error('Use a password with at least 6 characters.')
      }

      const endpoint = authMode === 'register' ? '/api/auth/register' : '/api/auth/login'
      const payload =
        authMode === 'register'
          ? { name: trimmedName, email: trimmedEmail, password: trimmedPassword }
          : { email: trimmedEmail, password: trimmedPassword }

      const response = await postJson(endpoint, payload, false)
      setCurrentUser({ ...response.user, token: response.token })
      setMessage(response.message)
      setAuthForm({ name: '', email: trimmedEmail, password: '' })
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreateProject(event) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const created = await postJson('/api/projects', projectForm)
      setProjectForm({
        name: 'Product Service',
        description: 'AI-generated CRUD service for internal catalog workflows',
      })
      setMessage('Project created successfully.')
      await loadWorkspace(created.id)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteProject(projectId) {
    setSubmitting(true)
    try {
      await deleteJson(`/api/projects/${projectId}`)
      const nextProjectId = Number(activeProjectId) === projectId
        ? projects.find((project) => project.id !== projectId)?.id ?? null
        : activeProjectId
      setMessage('Project deleted.')
      await loadWorkspace(nextProjectId)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleGenerateApi(event) {
    event.preventDefault()
    if (!activeProjectId) {
      setMessage('Create a project first so generated files have somewhere to be stored.')
      return
    }

    setSubmitting(true)
    try {
      await postJson('/api/generation', {
        projectId: Number(activeProjectId),
        entityName: generatorForm.entityName,
        description: generatorForm.description,
        rawPrompt: generatorForm.rawPrompt,
        includeJwt: generatorForm.includeJwt,
        multiEntity: generatorForm.multiEntity,
        fields: generatorForm.fields,
      })

      setMessage('API generated and saved successfully.')
      await loadWorkspace(activeProjectId)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteApi(apiId) {
    setSubmitting(true)
    try {
      await deleteJson(`/api/generation/${apiId}`)
      setMessage('Generated API deleted.')
      await loadWorkspace(activeProjectId)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDownloadZip(apiId) {
    try {
      const name = `${generatorForm.entityName || 'generated-api'}.zip`
      const response = await fetch(buildApiUrl(`/api/generation/${apiId}/zip?name=${encodeURIComponent(generatorForm.entityName || 'generated-api')}`), {
        headers: buildAuthHeaders(),
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.message || 'Unable to download ZIP.')
      }

      const blob = await response.blob()
      const blobUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = name
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(blobUrl)
    } catch (error) {
      setMessage(error.message)
    }
  }

  async function handleApiTest(event) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const trimmedUrl = testerForm.url.trim()
      if (!trimmedUrl) {
        throw new Error(`Enter a full URL for the API tester, for example ${apiOriginLabel}/api/products`)
      }
      if (!/^https?:\/\//i.test(trimmedUrl)) {
        throw new Error('Use a full URL starting with http:// or https:// in the API tester.')
      }

      const parsedHeaders = testerForm.headers ? JSON.parse(testerForm.headers) : {}
      const needsBackendAuth = isBackendUrl(trimmedUrl)
      const requestHeaders =
        needsBackendAuth && currentUser?.token && !parsedHeaders.Authorization
          ? { ...parsedHeaders, Authorization: `Bearer ${currentUser.token}` }
          : parsedHeaders

      const response = await postJson('/api/tester', {
        url: trimmedUrl,
        method: testerForm.method,
        headers: requestHeaders,
        body: testerForm.body,
      })
      setTesterResponse(response)
      setMessage('Request executed successfully.')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setSubmitting(false)
    }
  }

  function addField() {
    setGeneratorForm((current) => ({
      ...current,
      fields: [...current.fields, { ...initialField }],
    }))
  }

  function updateField(index, key, value) {
    setGeneratorForm((current) => ({
      ...current,
      fields: current.fields.map((field, fieldIndex) =>
        fieldIndex === index ? { ...field, [key]: value } : field,
      ),
    }))
  }

  function removeField(index) {
    setGeneratorForm((current) => ({
      ...current,
      fields: current.fields.filter((_, fieldIndex) => fieldIndex !== index),
    }))
  }

  function handleLogout(customMessage = 'Logged out successfully.') {
    window.localStorage.removeItem(storageKey)
    setCurrentUser(null)
    setSummary(null)
    setProjects([])
    setApiHistory([])
    setActiveProjectId('')
    setActiveApiId(null)
    setSelectedFileIndex(0)
    setMessage(customMessage)
  }

  if (!currentUser?.token) {
    return (
      <div className="auth-shell">
        <div className="auth-panel">
          <div className="auth-copy">
            <p className="eyebrow">CodeForge AI</p>
            <h1 className="auth-title">CodeForge AI</h1>
            <p className="auth-subtitle">
              Sign in first, then create projects, generate Spring Boot code, preview files,
              and store everything inside your database workspace.
            </p>
          </div>

          <div className="auth-card">
            <div className="auth-tabs">
              <button
                type="button"
                className={authMode === 'login' ? 'active' : ''}
                onClick={() => setAuthMode('login')}
              >
                Login
              </button>
              <button
                type="button"
                className={authMode === 'register' ? 'active' : ''}
                onClick={() => setAuthMode('register')}
              >
                Register
              </button>
            </div>

            <form className="stack" onSubmit={handleAuthSubmit}>
              {authMode === 'register' ? (
                <label>
                  Full name
                  <input
                    value={authForm.name}
                    onChange={(event) => setAuthForm({ ...authForm, name: event.target.value })}
                  />
                </label>
              ) : null}

              <label>
                Email
                <input
                  type="email"
                  value={authForm.email}
                  onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
                />
              </label>

              <label>
                Password
                <input
                  type="password"
                  value={authForm.password}
                  onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                />
              </label>

              <button type="submit" disabled={submitting}>
                {authMode === 'register' ? 'Create account' : 'Sign in'}
              </button>
            </form>

            {message ? <div className="message-bar auth-message">{message}</div> : null}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="app-shell">
      <header className="hero-panel">
        <div className="hero-content">
          <p className="eyebrow">CodeForge AI</p>
          <h1 className="hero-title">CodeForge AI</h1>
          <p className="hero-copy">
            Welcome back, {currentUser.name}. Your workspace now supports JWT auth, project management,
            generated API cleanup, ZIP exports, and a protected backend pipeline.
          </p>
        </div>
        <div className="hero-card">
          <span>Architecture Flow</span>
          <strong>Prompt Builder - OpenAI - File Splitter - ZIP - MySQL</strong>
          <p>Built for Java 17, Spring Boot, Maven, JPA, Hibernate, and a polished generation dashboard.</p>
          <button type="button" className="logout-button" onClick={() => handleLogout()}>Logout</button>
        </div>
      </header>

      <section className="stats-grid">
        {[
          ['My Account', currentUser?.id ?? '-'],
          ['My Projects', summary?.projectCount ?? 0],
          ['My APIs', summary?.apiCount ?? 0],
          ['My Files', summary?.fileCount ?? 0],
        ].map(([label, value]) => (
          <article className="stat-card" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
          </article>
        ))}
      </section>

      {message ? <div className="message-bar">{message}</div> : null}

      <section className="dashboard-grid">
        <article className="dashboard-card account-overview">
          <div className="panel-heading">
            <h2>Account Overview</h2>
            <p>Your profile and current workspace access.</p>
          </div>
          <div className="identity-block">
            <div className="identity-avatar">
              {(currentUser?.name || currentUser?.email || 'U').charAt(0).toUpperCase()}
            </div>
            <div>
              <strong>{currentUser?.name || 'Unknown User'}</strong>
              <span>{currentUser?.email || 'No email available'}</span>
            </div>
          </div>
          <div className="dashboard-detail-list">
            <div className="detail-item">
              <span>Account ID</span>
              <strong>{currentUser?.id ?? 'Not available'}</strong>
            </div>
            <div className="detail-item">
              <span>Total Projects</span>
              <strong>{summary?.projectCount ?? projects.length}</strong>
            </div>
            <div className="detail-item">
              <span>Total Generated APIs</span>
              <strong>{summary?.apiCount ?? 0}</strong>
            </div>
            <div className="detail-item">
              <span>Total Stored Files</span>
              <strong>{summary?.fileCount ?? 0}</strong>
            </div>
          </div>
        </article>

        <article className="dashboard-card project-overview">
          <div className="panel-heading">
            <h2>Selected Project Details</h2>
            <p>See the currently active project clearly before generating or testing APIs.</p>
          </div>
          {selectedProject ? (
            <>
              <div className="highlight-card">
                <p className="viewer-label">Active Project</p>
                <strong>{selectedProject.name}</strong>
                <span>{selectedProject.description || 'No description added for this project yet.'}</span>
              </div>
              <div className="dashboard-detail-list project-detail-grid">
                <div className="detail-item">
                  <span>Project ID</span>
                  <strong>{selectedProject.id}</strong>
                </div>
                <div className="detail-item">
                  <span>Generated APIs</span>
                  <strong>{selectedProject.apiCount ?? apiHistory.length}</strong>
                </div>
                <div className="detail-item">
                  <span>Stored Files</span>
                  <strong>{selectedProject.fileCount ?? activeApi?.files?.length ?? 0}</strong>
                </div>
                <div className="detail-item">
                  <span>Current Status</span>
                  <strong>{loading ? 'Loading' : 'Ready'}</strong>
                </div>
              </div>
            </>
          ) : (
            <p className="empty-state">Select or create a project to see its details here.</p>
          )}
        </article>

        <article className="dashboard-card project-table-card">
          <div className="panel-heading">
            <h2>Project Dashboard</h2>
            <p>Quickly review all projects with their API and file totals.</p>
          </div>
          {projects.length ? (
            <div className="project-dashboard-list">
              {projects.map((project) => (
                <button
                  key={project.id}
                  type="button"
                  className={`project-dashboard-row ${Number(activeProjectId) === project.id ? 'active' : ''}`}
                  onClick={async () => {
                    setActiveProjectId(project.id)
                    await loadProjectApis(project.id)
                  }}
                >
                  <div>
                    <strong>{project.name}</strong>
                    <span>{project.description || 'No description yet'}</span>
                  </div>
                  <div className="project-dashboard-metrics">
                    <span>{project.apiCount ?? 0} APIs</span>
                    <span>{project.fileCount ?? 0} files</span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <p className="empty-state">No projects yet. Create one to start building your dashboard.</p>
          )}
        </article>
      </section>

      <main className="workspace-grid">
        <section className="panel">
          <div className="panel-heading">
            <h2>Create Project</h2>
            <p>Add a new workspace and keep your generated APIs organized.</p>
          </div>

          <form className="stack" onSubmit={handleCreateProject}>
            <label>
              Project name
              <input
                value={projectForm.name}
                onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })}
              />
            </label>

            <label>
              Description
              <textarea
                rows="4"
                value={projectForm.description}
                onChange={(event) => setProjectForm({ ...projectForm, description: event.target.value })}
              />
            </label>

            <button type="submit" disabled={submitting}>Create project</button>
          </form>
        </section>

        <section className="panel panel-large">
          <div className="panel-heading">
            <h2>Create API</h2>
            <p>Enhance raw prompts into a structured Spring Boot generation request.</p>
          </div>

          <form className="stack" onSubmit={handleGenerateApi}>
            <label>
              Active project
              <select
                value={activeProjectId}
                onChange={async (event) => {
                  const value = event.target.value
                  setActiveProjectId(value)
                  await loadProjectApis(value)
                }}
              >
                <option value="">Select project</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </label>

            <div className="form-grid">
              <label>
                Entity name
                <input
                  value={generatorForm.entityName}
                  onChange={(event) => setGeneratorForm({ ...generatorForm, entityName: event.target.value })}
                />
              </label>
              <label>
                Raw prompt
                <input
                  value={generatorForm.rawPrompt}
                  onChange={(event) => setGeneratorForm({ ...generatorForm, rawPrompt: event.target.value })}
                />
              </label>
            </div>

            <label>
              Functional description
              <textarea
                rows="4"
                value={generatorForm.description}
                onChange={(event) => setGeneratorForm({ ...generatorForm, description: event.target.value })}
              />
            </label>

            <div className="toggle-row">
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={generatorForm.includeJwt}
                  onChange={(event) => setGeneratorForm({ ...generatorForm, includeJwt: event.target.checked })}
                />
                Include JWT starter support
              </label>
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={generatorForm.multiEntity}
                  onChange={(event) => setGeneratorForm({ ...generatorForm, multiEntity: event.target.checked })}
                />
                Multi-entity aware prompt
              </label>
            </div>

            <div className="field-builder">
              <div className="subheading">
                <h3>Field Builder</h3>
                <button type="button" className="secondary" onClick={addField}>Add field</button>
              </div>
              {generatorForm.fields.map((field, index) => (
                <div className="field-row" key={`${field.name}-${index}`}>
                  <input
                    placeholder="name"
                    value={field.name}
                    onChange={(event) => updateField(index, 'name', event.target.value)}
                  />
                  <input
                    placeholder="type"
                    value={field.type}
                    onChange={(event) => updateField(index, 'type', event.target.value)}
                  />
                  <label className="checkbox compact">
                    <input
                      type="checkbox"
                      checked={field.required}
                      onChange={(event) => updateField(index, 'required', event.target.checked)}
                    />
                    Required
                  </label>
                  <button type="button" className="ghost" onClick={() => removeField(index)}>Remove</button>
                </div>
              ))}
            </div>

            <button type="submit" disabled={submitting || !activeProjectId}>Generate API</button>
          </form>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <h2>My Projects</h2>
            <p>Select a project to inspect its details or remove old workspaces.</p>
          </div>
          <div className="project-list">
            {projects.map((project) => (
              <div
                key={project.id}
                className={`project-card ${Number(activeProjectId) === project.id ? 'active' : ''}`}
              >
                <button
                  type="button"
                  className="project-select"
                  onClick={async () => {
                    setActiveProjectId(project.id)
                    await loadProjectApis(project.id)
                  }}
                >
                  <strong>{project.name}</strong>
                  <span>{project.description || 'No description yet'}</span>
                  <small>{project.apiCount} APIs | {project.fileCount} files</small>
                </button>
                <button
                  type="button"
                  className="danger-button"
                  onClick={() => handleDeleteProject(project.id)}
                  disabled={submitting}
                >
                  Delete
                </button>
              </div>
            ))}
            {!projects.length && <p className="empty-state">Create your first project to start generating APIs.</p>}
          </div>
        </section>

        <section className="panel panel-large">
          <div className="panel-heading">
            <h2>Generated Code</h2>
            <p>Preview files, inspect prompts, download ZIPs, and remove old generations.</p>
          </div>

          <div className="history-strip">
            {apiHistory.map((api) => (
              <button
                key={api.id}
                type="button"
                className={`history-pill ${activeApi?.id === api.id ? 'active' : ''}`}
                onClick={() => {
                  setActiveApiId(api.id)
                  setSelectedFileIndex(0)
                }}
              >
                #{api.id}
              </button>
            ))}
          </div>

          {activeApi ? (
            <div className="viewer-grid">
              <div className="viewer-sidebar">
                <p className="viewer-label">Prompt</p>
                <pre>{activeApi.prompt}</pre>
                <p className="viewer-label">Files</p>
                <div className="file-list">
                  {activeApi.files.map((file, index) => (
                    <button
                      key={file.fileName}
                      type="button"
                      className={selectedFileIndex === index ? 'active' : ''}
                      onClick={() => setSelectedFileIndex(index)}
                    >
                      {file.fileName}
                    </button>
                  ))}
                </div>
                <div className="viewer-actions">
                  <button type="button" onClick={() => handleDownloadZip(activeApi.id)}>Download ZIP</button>
                  <button
                    type="button"
                    className="danger-button"
                    onClick={() => handleDeleteApi(activeApi.id)}
                    disabled={submitting}
                  >
                    Delete API
                  </button>
                </div>
              </div>

              <div className="code-viewer">
                <div className="code-header">
                  <strong>{activeFile?.fileName ?? 'No file selected'}</strong>
                  <span>{activeApi.files.length} files</span>
                </div>
                <pre>{activeFile?.fileContent ?? 'Nothing generated yet.'}</pre>
              </div>
            </div>
          ) : (
            <p className="empty-state">Generated APIs will appear here once you submit a prompt.</p>
          )}
        </section>

        <section className="panel panel-wide">
          <div className="panel-heading">
            <h2>API Tester</h2>
            <p>Use the backend as a small Postman-style relay for quick checks.</p>
          </div>

          <form className="stack" onSubmit={handleApiTest}>
            <div className="form-grid tester-grid">
              <label>
                URL
                <input
                  placeholder="http://127.0.0.1:8080/api/products"
                  value={testerForm.url}
                  onChange={(event) => setTesterForm({ ...testerForm, url: event.target.value })}
                />
              </label>
              <label>
                Method
                <select
                  value={testerForm.method}
                  onChange={(event) => setTesterForm({ ...testerForm, method: event.target.value })}
                >
                  {['GET', 'POST', 'PUT', 'DELETE'].map((method) => (
                    <option key={method} value={method}>{method}</option>
                  ))}
                </select>
              </label>
            </div>

            <label>
              Headers JSON
              <textarea
                rows="4"
                value={testerForm.headers}
                onChange={(event) => setTesterForm({ ...testerForm, headers: event.target.value })}
              />
            </label>

            <label>
              Body
              <textarea
                rows="5"
                placeholder="Leave empty for GET. Add JSON only for POST/PUT requests."
                value={testerForm.body}
                onChange={(event) => setTesterForm({ ...testerForm, body: event.target.value })}
              />
            </label>

            <button type="submit" disabled={submitting}>Send request</button>
          </form>

          <div className="response-box">
            <div className="code-header">
              <strong>Response</strong>
              <span>Status: {testerResponse?.status ?? 'Not sent'}</span>
            </div>
            <pre>{testerResponse ? JSON.stringify(testerResponse, null, 2) : 'Run a request to see the response payload here.'}</pre>
          </div>
        </section>
      </main>

      <footer className="footer-note">
        <span>Backend: JWT-secured Spring Boot</span>
        <span>Database: H2 local or Aiven profile</span>
        <span>Frontend: React workspace</span>
        <span>Status: {loading ? 'Loading workspace...' : 'Ready'}</span>
      </footer>
    </div>
  )
}

async function fetchJson(url, useAuth = true) {
  try {
    const response = await fetch(buildApiUrl(url), {
      headers: useAuth ? buildAuthHeaders() : {},
    })
    const data = await parseResponseBody(response)
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        throw new Error('Your session has expired or you are not authorized. Please log in again.')
      }
      throw new Error(resolveApiErrorMessage(response, data, url))
    }
    return data
  } catch (error) {
    throw normalizeNetworkError(error)
  }
}

async function postJson(url, body, useAuth = true) {
  try {
    const response = await fetch(buildApiUrl(url), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(useAuth ? buildAuthHeaders() : {}),
      },
      body: JSON.stringify(body),
    })

    const data = await parseResponseBody(response)
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        throw new Error('Your session has expired or you are not authorized. Please log in again.')
      }
      throw new Error(resolveApiErrorMessage(response, data, url))
    }
    return data
  } catch (error) {
    throw normalizeNetworkError(error)
  }
}

async function deleteJson(url) {
  try {
    const response = await fetch(buildApiUrl(url), {
      method: 'DELETE',
      headers: buildAuthHeaders(),
    })

    if (!response.ok) {
      const data = await parseResponseBody(response)
      if (response.status === 401 || response.status === 403) {
        throw new Error('Your session has expired or you are not authorized. Please log in again.')
      }
      throw new Error(resolveApiErrorMessage(response, data, url))
    }
  } catch (error) {
    throw normalizeNetworkError(error)
  }
}

function buildApiUrl(url) {
  if (/^https?:\/\//i.test(url)) {
    return url
  }

  return `${apiBaseUrl}${url}`
}

async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    return response.json().catch(() => ({}))
  }

  const text = await response.text().catch(() => '')
  return text ? { raw: text } : {}
}

function resolveApiErrorMessage(response, data, url) {
  if (typeof data?.message === 'string' && data.message.trim()) {
    return data.message
  }

  if (typeof data?.error === 'string' && data.error.trim()) {
    return data.error
  }

  if (typeof data?.raw === 'string') {
    const raw = data.raw.trim()

    if (!raw) {
      return `${response.status} ${response.statusText}: request failed for ${url}`
    }
    if (/proxy error|ECONNREFUSED|connect ECONNREFUSED/i.test(raw)) {
      return `Backend is not reachable on ${apiOriginLabel}. Check that the deployed API is running and that VITE_API_BASE_URL is correct.`
    }
    if (/^<!doctype html>|^<html/i.test(raw)) {
      return `The backend returned an unexpected HTML error page. Check that ${apiOriginLabel} is serving the Spring Boot API correctly.`
    }

    return raw
  }

  return `${response.status} ${response.statusText}: request failed for ${url}`
}

function normalizeNetworkError(error) {
  if (error instanceof TypeError) {
    return new Error(`Backend is not reachable on ${apiOriginLabel}. Check that the deployed API is running and that VITE_API_BASE_URL is correct.`)
  }

  return error
}

function buildAuthHeaders() {
  const saved = window.localStorage.getItem(storageKey)
  if (!saved) {
    return {}
  }

  let parsed
  try {
    parsed = JSON.parse(saved)
  } catch {
    window.localStorage.removeItem(storageKey)
    return {}
  }

  if (!parsed?.token) {
    return {}
  }

  return {
    Authorization: `Bearer ${parsed.token}`,
  }
}

function deriveTesterPreset(activeApi) {
  if (!activeApi?.files?.length) {
    return null
  }

  const controllerFile = activeApi.files.find((file) => file.fileName.endsWith('Controller.java'))
  if (!controllerFile?.fileContent) {
    return null
  }

  const mappingMatch = controllerFile.fileContent.match(/@RequestMapping\("([^"]+)"\)/)
  if (!mappingMatch?.[1]) {
    return null
  }

  return {
    url: `${apiOriginLabel}${mappingMatch[1]}`,
  }
}

function isBackendUrl(url) {
  try {
    const target = new URL(url)

    if (!apiBaseUrl) {
      return /^(127\.0\.0\.1|localhost)$/i.test(target.hostname) && target.port === '8080'
    }

    return target.origin === apiBaseUrl
  } catch {
    return false
  }
}

export default App
